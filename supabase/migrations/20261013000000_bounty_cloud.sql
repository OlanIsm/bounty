create table public.profiles (
    id uuid primary key references auth.users(id) on delete cascade,
    display_name text not null check (char_length(trim(display_name)) between 1 and 80),
    demo_balance bigint not null default 50000 check (demo_balance >= 0),
    created_at timestamptz not null default now()
);

create table public.challenges (
    id uuid primary key default gen_random_uuid(),
    creator_id uuid not null references public.profiles(id),
    creator_name text not null,
    title text not null check (char_length(trim(title)) between 1 and 100),
    description text not null check (char_length(trim(description)) between 1 and 2000),
    reward_amount bigint not null check (reward_amount > 0),
    deadline date not null,
    status text not null default 'OPEN'
        check (status in ('OPEN', 'ACCEPTED', 'SUBMITTED', 'COMPLETED')),
    participant_id uuid references public.profiles(id),
    participant_name text,
    created_at timestamptz not null default now(),
    check ((participant_id is null) = (participant_name is null))
);

create table public.proofs (
    id uuid primary key default gen_random_uuid(),
    challenge_id uuid not null references public.challenges(id) on delete cascade,
    hunter_id uuid not null references public.profiles(id),
    description text not null check (char_length(trim(description)) between 1 and 2000),
    image_path text not null,
    status text not null default 'PENDING'
        check (status in ('PENDING', 'APPROVED', 'REJECTED')),
    submitted_at timestamptz not null default now()
);

create index challenges_created_at_idx on public.challenges (created_at desc);
create index challenges_creator_id_idx on public.challenges (creator_id);
create index challenges_participant_id_idx on public.challenges (participant_id);
create index proofs_challenge_submitted_idx on public.proofs (challenge_id, submitted_at desc, id desc);

create table public.mock_reward_receipts (
    challenge_id uuid primary key references public.challenges(id),
    hunter_id uuid not null references public.profiles(id),
    proof_id uuid not null unique references public.proofs(id),
    amount bigint not null check (amount > 0),
    credited_at timestamptz not null default now()
);

alter table public.profiles enable row level security;
alter table public.challenges enable row level security;
alter table public.proofs enable row level security;
alter table public.mock_reward_receipts enable row level security;

grant usage on schema public to authenticated;
grant select on public.profiles, public.challenges, public.proofs, public.mock_reward_receipts to authenticated;
grant insert on public.challenges to authenticated;
grant update (display_name) on public.profiles to authenticated;

create policy "profiles_read_own" on public.profiles
    for select to authenticated using (id = (select auth.uid()));
create policy "profiles_update_own_name" on public.profiles
    for update to authenticated using (id = (select auth.uid()))
    with check (id = (select auth.uid()));

create policy "challenges_read_authenticated" on public.challenges
    for select to authenticated using (true);
create policy "challenges_create_as_creator" on public.challenges
    for insert to authenticated with check (
        creator_id = (select auth.uid()) and status = 'OPEN'
        and participant_id is null and participant_name is null
    );

create policy "proofs_read_as_challenge_member" on public.proofs
    for select to authenticated using (
        exists (
            select 1 from public.challenges c
            where c.id = challenge_id
              and ((select auth.uid()) = c.creator_id or (select auth.uid()) = c.participant_id)
        )
    );

create policy "reward_receipts_read_as_hunter" on public.mock_reward_receipts
    for select to authenticated using (hunter_id = (select auth.uid()));

create or replace function public.create_profile_for_user()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
    insert into public.profiles (id, display_name)
    values (
        new.id,
        coalesce(nullif(trim(new.raw_user_meta_data ->> 'name'), ''), split_part(new.email, '@', 1))
    );
    return new;
end;
$$;

create trigger on_auth_user_created
    after insert on auth.users
    for each row execute function public.create_profile_for_user();

create or replace function public.accept_challenge(target_challenge uuid)
returns boolean
language plpgsql
security definer
set search_path = ''
as $$
begin
    if (select auth.uid()) is null then
        raise exception using errcode = '28000', message = 'Authentication required';
    end if;

    update public.challenges c
    set status = 'ACCEPTED',
        participant_id = (select auth.uid()),
        participant_name = (select p.display_name from public.profiles p where p.id = (select auth.uid()))
    where c.id = target_challenge
      and c.status = 'OPEN'
      and c.creator_id <> (select auth.uid());

    return found;
end;
$$;

create or replace function public.submit_proof(
    target_challenge uuid,
    proof_description text,
    proof_image_path text
)
returns uuid
language plpgsql
security definer
set search_path = ''
as $$
declare
    new_proof_id uuid;
begin
    if (select auth.uid()) is null then
        raise exception using errcode = '28000', message = 'Authentication required';
    end if;
    if char_length(trim(proof_description)) not between 1 and 2000
       or proof_image_path !~* ('^' || (select auth.uid())::text || '/' || target_challenge::text || '/[^/]+\.(jpg|jpeg|png|webp)$') then
        raise exception using errcode = '22023', message = 'Invalid proof';
    end if;

    update public.challenges c
    set status = 'SUBMITTED'
    where c.id = target_challenge
      and c.participant_id = (select auth.uid())
      and c.status = 'ACCEPTED';
    if not found then
        raise exception using errcode = 'P0001', message = 'Challenge is not available for proof submission';
    end if;

    insert into public.proofs (challenge_id, hunter_id, description, image_path)
    values (target_challenge, (select auth.uid()), trim(proof_description), proof_image_path)
    returning id into new_proof_id;
    return new_proof_id;
end;
$$;

create or replace function public.review_proof(
    target_challenge uuid,
    target_proof uuid,
    approve boolean
)
returns boolean
language plpgsql
security definer
set search_path = ''
as $$
declare
    challenge_row public.challenges%rowtype;
    proof_row public.proofs%rowtype;
    credited_challenge uuid;
begin
    if (select auth.uid()) is null then
        raise exception using errcode = '28000', message = 'Authentication required';
    end if;

    select * into challenge_row from public.challenges c
    where c.id = target_challenge for update;
    select * into proof_row from public.proofs p
    where p.challenge_id = target_challenge
    order by p.submitted_at desc, p.id desc limit 1 for update;

    if challenge_row.id is null or challenge_row.creator_id <> (select auth.uid())
       or challenge_row.status <> 'SUBMITTED'
       or proof_row.id is null or proof_row.id <> target_proof
       or proof_row.status <> 'PENDING'
       or proof_row.hunter_id <> challenge_row.participant_id then
        return false;
    end if;

    update public.proofs set status = case when approve then 'APPROVED' else 'REJECTED' end
    where id = target_proof;
    update public.challenges set status = case when approve then 'COMPLETED' else 'ACCEPTED' end
    where id = target_challenge;

    if approve then
        insert into public.mock_reward_receipts (challenge_id, hunter_id, proof_id, amount)
        values (target_challenge, challenge_row.participant_id, target_proof, challenge_row.reward_amount)
        on conflict do nothing
        returning challenge_id into credited_challenge;
        if credited_challenge is not null then
            update public.profiles
            set demo_balance = demo_balance + challenge_row.reward_amount
            where id = challenge_row.participant_id;
        end if;
    end if;

    return true;
end;
$$;

revoke all on function public.create_profile_for_user() from public, anon, authenticated;
revoke all on function public.accept_challenge(uuid) from public, anon;
revoke all on function public.submit_proof(uuid, text, text) from public, anon;
revoke all on function public.review_proof(uuid, uuid, boolean) from public, anon;
grant execute on function public.accept_challenge(uuid) to authenticated;
grant execute on function public.submit_proof(uuid, text, text) to authenticated;
grant execute on function public.review_proof(uuid, uuid, boolean) to authenticated;

insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values ('bounty-proof-photos', 'bounty-proof-photos', false, 5242880,
        array['image/jpeg', 'image/png', 'image/webp'])
on conflict (id) do nothing;

create policy "proof_photos_upload_as_hunter" on storage.objects
    for insert to authenticated with check (
        bucket_id = 'bounty-proof-photos'
        and (storage.foldername(name))[1] = (select auth.uid())::text
        and exists (
            select 1 from public.challenges c
            where c.id::text = (storage.foldername(name))[2]
              and c.participant_id = (select auth.uid())
              and c.status = 'ACCEPTED'
        )
    );

create policy "proof_photos_read_as_challenge_member" on storage.objects
    for select to authenticated using (
        bucket_id = 'bounty-proof-photos'
        and exists (
            select 1 from public.challenges c
            where c.id::text = (storage.foldername(name))[2]
              and ((select auth.uid()) = c.creator_id or (select auth.uid()) = c.participant_id)
        )
    );
