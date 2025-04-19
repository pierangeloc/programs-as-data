create table public.shop
(
    id         integer,
    name       text,
    address    text,
    country    text,
    categories jsonb
);

alter table public.shop
    owner to postgres;




INSERT INTO public.shop (id, name, address, country, categories) VALUES (1, 'electronics paradise', 'something', 'UK', '["electronics"]');
INSERT INTO public.shop (id, name, address, country, categories) VALUES (2, 'food paradise', 'something', 'FR', '["food"]');
INSERT INTO public.shop (id, name, address, country, categories) VALUES (3, 'entertainement heaven', 'something', 'US', '["entertainement", "electronics"]');
INSERT INTO public.shop (id, name, address, country, categories) VALUES (4, 'china electronics inc', 'something', 'CH', '["electronics"]');
INSERT INTO public.shop (id, name, address, country, categories) VALUES (5, 'venice casino', 'something', 'IT', '["gambling"]');
