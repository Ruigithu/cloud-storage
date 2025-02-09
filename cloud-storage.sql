--
-- PostgreSQL database dump
--

-- Dumped from database version 17.0
-- Dumped by pg_dump version 17.0

-- Started on 2025-02-06 10:12:37

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET transaction_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- TOC entry 2 (class 3079 OID 16674)
-- Name: ltree; Type: EXTENSION; Schema: -; Owner: -
--

CREATE EXTENSION IF NOT EXISTS ltree WITH SCHEMA public;


--
-- TOC entry 5040 (class 0 OID 0)
-- Dependencies: 2
-- Name: EXTENSION ltree; Type: COMMENT; Schema: -; Owner: 
--

COMMENT ON EXTENSION ltree IS 'data type for hierarchical tree-like structures';


--
-- TOC entry 963 (class 1247 OID 16931)
-- Name: permission_type; Type: TYPE; Schema: public; Owner: postgres
--

CREATE TYPE public.permission_type AS ENUM (
    'read',
    'write',
    'admin'
);


ALTER TYPE public.permission_type OWNER TO postgres;

--
-- TOC entry 309 (class 1255 OID 16996)
-- Name: update_updated_at_column(); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.update_updated_at_column() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$;


ALTER FUNCTION public.update_updated_at_column() OWNER TO postgres;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- TOC entry 229 (class 1259 OID 16971)
-- Name: activity_logs; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.activity_logs (
    id integer NOT NULL,
    user_id integer NOT NULL,
    action_type character varying(50) NOT NULL,
    resource_type character varying(50) NOT NULL,
    resource_id integer NOT NULL,
    details jsonb,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.activity_logs OWNER TO postgres;

--
-- TOC entry 228 (class 1259 OID 16970)
-- Name: activity_logs_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.activity_logs_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.activity_logs_id_seq OWNER TO postgres;

--
-- TOC entry 5041 (class 0 OID 0)
-- Dependencies: 228
-- Name: activity_logs_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.activity_logs_id_seq OWNED BY public.activity_logs.id;


--
-- TOC entry 227 (class 1259 OID 16938)
-- Name: file_permissions; Type: TABLE; Schema: public; Owner: postgres
--




--
-- TOC entry 225 (class 1259 OID 16909)
-- Name: file_versions; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.file_versions (
    id integer NOT NULL,
    file_id integer NOT NULL,
    version_number integer NOT NULL,
    storage_path text NOT NULL,
    size bigint NOT NULL,
    created_by integer NOT NULL,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    comment text
);


ALTER TABLE public.file_versions OWNER TO postgres;

--
-- TOC entry 224 (class 1259 OID 16908)
-- Name: file_versions_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.file_versions_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.file_versions_id_seq OWNER TO postgres;

--
-- TOC entry 5043 (class 0 OID 0)
-- Dependencies: 224
-- Name: file_versions_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.file_versions_id_seq OWNED BY public.file_versions.id;


--
-- TOC entry 223 (class 1259 OID 16889)
-- Name: files; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.files (
    id integer NOT NULL,
    name character varying(255) NOT NULL,
    folder_id integer,
    owner_id integer NOT NULL,
    mime_type character varying(100),
    size bigint NOT NULL,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    is_deleted boolean DEFAULT false
);


ALTER TABLE public.files OWNER TO postgres;

--
-- TOC entry 222 (class 1259 OID 16888)
-- Name: files_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.files_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.files_id_seq OWNER TO postgres;

--
-- TOC entry 5044 (class 0 OID 0)
-- Dependencies: 222
-- Name: files_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.files_id_seq OWNED BY public.files.id;


--
-- TOC entry 221 (class 1259 OID 16867)
-- Name: folders; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.folders (
    id integer NOT NULL,
    name character varying(255) NOT NULL,
    parent_id integer,
    owner_id integer NOT NULL,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    path public.ltree NOT NULL,
    is_deleted boolean DEFAULT false
);


ALTER TABLE public.folders OWNER TO postgres;

--
-- TOC entry 220 (class 1259 OID 16866)
-- Name: folders_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.folders_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.folders_id_seq OWNER TO postgres;

--
-- TOC entry 5045 (class 0 OID 0)
-- Dependencies: 220
-- Name: folders_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.folders_id_seq OWNED BY public.folders.id;


--
-- TOC entry 219 (class 1259 OID 16662)
-- Name: users; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.users (
    id integer NOT NULL,
    email character varying(255) NOT NULL,
    password_hash character varying(255) NOT NULL,
    name character varying(100) NOT NULL,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.users OWNER TO postgres;

--
-- TOC entry 218 (class 1259 OID 16661)
-- Name: users_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.users_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.users_id_seq OWNER TO postgres;

--
-- TOC entry 5046 (class 0 OID 0)
-- Dependencies: 218
-- Name: users_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.users_id_seq OWNED BY public.users.id;


--
-- TOC entry 4831 (class 2604 OID 16974)
-- Name: activity_logs id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.activity_logs ALTER COLUMN id SET DEFAULT nextval('public.activity_logs_id_seq'::regclass);


--
-- TOC entry 4829 (class 2604 OID 16941)
-- Name: file_permissions id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.file_permissions ALTER COLUMN id SET DEFAULT nextval('public.file_permissions_id_seq'::regclass);


--
-- TOC entry 4827 (class 2604 OID 16912)
-- Name: file_versions id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.file_versions ALTER COLUMN id SET DEFAULT nextval('public.file_versions_id_seq'::regclass);


--
-- TOC entry 4823 (class 2604 OID 16892)
-- Name: files id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.files ALTER COLUMN id SET DEFAULT nextval('public.files_id_seq'::regclass);


--
-- TOC entry 4819 (class 2604 OID 16870)
-- Name: folders id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.folders ALTER COLUMN id SET DEFAULT nextval('public.folders_id_seq'::regclass);


--
-- TOC entry 4816 (class 2604 OID 16665)
-- Name: users id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.users ALTER COLUMN id SET DEFAULT nextval('public.users_id_seq'::regclass);


--
-- TOC entry 5034 (class 0 OID 16971)
-- Dependencies: 229
-- Data for Name: activity_logs; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.activity_logs (id, user_id, action_type, resource_type, resource_id, details, created_at) FROM stdin;
\.


--
-- TOC entry 5032 (class 0 OID 16938)
-- Dependencies: 227
-- Data for Name: file_permissions; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.file_permissions (id, file_id, folder_id, user_id, permission, created_at, created_by) FROM stdin;
138	\N	111	1	admin	2025-02-05 17:34:08.474539+00	1
139	\N	112	1	admin	2025-02-05 17:34:08.490986+00	1
\.


--
-- TOC entry 5030 (class 0 OID 16909)
-- Dependencies: 225
-- Data for Name: file_versions; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.file_versions (id, file_id, version_number, storage_path, size, created_by, created_at, comment) FROM stdin;
70	91	1	E:\\data\\cloud-storage\\1\\111\\test\\w1-thesis_v1.doc	35840	1	2025-02-05 17:34:08.524314+00	\N
71	92	1	E:\\data\\cloud-storage\\1\\111\\test\\Week 2_v1.docx	17444	1	2025-02-05 17:34:08.534144+00	\N
72	93	1	E:\\data\\cloud-storage\\1\\111\\test\\~$Week 2_v1.docx	162	1	2025-02-05 17:34:08.547042+00	\N
\.


--
-- TOC entry 5028 (class 0 OID 16889)
-- Dependencies: 223
-- Data for Name: files; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.files (id, name, folder_id, owner_id, mime_type, size, created_at, updated_at, is_deleted) FROM stdin;
91	w1-thesis.doc	111	1	application/msword	35840	2025-02-05 17:34:08.506191+00	2025-02-05 17:34:08.506191+00	f
92	Week 2.docx	111	1	application/vnd.openxmlformats-officedocument.wordprocessingml.document	17444	2025-02-05 17:34:08.528142+00	2025-02-05 17:34:08.528142+00	f
93	~$Week 2.docx	111	1	application/vnd.openxmlformats-officedocument.wordprocessingml.document	162	2025-02-05 17:34:08.540896+00	2025-02-05 17:34:08.540896+00	f
\.


--
-- TOC entry 5026 (class 0 OID 16867)
-- Dependencies: 221
-- Data for Name: folders; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.folders (id, name, parent_id, owner_id, created_at, updated_at, path, is_deleted) FROM stdin;
111	test	1	1	2025-02-05 17:34:08.451587+00	2025-02-05 17:34:08.465407+00	111	f
112	test1	111	1	2025-02-05 17:34:08.478524+00	2025-02-05 17:34:08.49121+00	111.1	f
1	Root	\N	1	2025-02-04 12:53:34.422243+00	2025-02-04 12:53:34.422243+00	0	f
\.


--
-- TOC entry 5024 (class 0 OID 16662)
-- Dependencies: 219
-- Data for Name: users; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.users (id, email, password_hash, name, created_at, updated_at) FROM stdin;
1	jadeisme667@gmail.com	$2a$16$2oU6uZVmEdxUuHIFkANlDuHty2QtYPdA3DV9OMo2fhrcYDPkoEylq	Jade	2025-01-29 16:32:14.173809+00	2025-01-29 16:32:14.173809+00
\.


--
-- TOC entry 5047 (class 0 OID 0)
-- Dependencies: 228
-- Name: activity_logs_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.activity_logs_id_seq', 1, false);


--
-- TOC entry 5048 (class 0 OID 0)
-- Dependencies: 226
-- Name: file_permissions_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.file_permissions_id_seq', 139, true);


--
-- TOC entry 5049 (class 0 OID 0)
-- Dependencies: 224
-- Name: file_versions_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.file_versions_id_seq', 73, true);


--
-- TOC entry 5050 (class 0 OID 0)
-- Dependencies: 222
-- Name: files_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.files_id_seq', 94, true);


--
-- TOC entry 5051 (class 0 OID 0)
-- Dependencies: 220
-- Name: folders_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.folders_id_seq', 112, true);


--
-- TOC entry 5052 (class 0 OID 0)
-- Dependencies: 218
-- Name: users_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.users_id_seq', 1, true);


--
-- TOC entry 4861 (class 2606 OID 16979)
-- Name: activity_logs activity_logs_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.activity_logs
    ADD CONSTRAINT activity_logs_pkey PRIMARY KEY (id);


--
-- TOC entry 4852 (class 2606 OID 16947)
-- Name: file_permissions file_permissions_file_id_user_id_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.file_permissions
    ADD CONSTRAINT file_permissions_file_id_user_id_key UNIQUE (file_id, user_id);


--
-- TOC entry 4854 (class 2606 OID 16949)
-- Name: file_permissions file_permissions_folder_id_user_id_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.file_permissions
    ADD CONSTRAINT file_permissions_folder_id_user_id_key UNIQUE (folder_id, user_id);


--
-- TOC entry 4856 (class 2606 OID 16945)
-- Name: file_permissions file_permissions_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.file_permissions
    ADD CONSTRAINT file_permissions_pkey PRIMARY KEY (id);


--
-- TOC entry 4848 (class 2606 OID 16919)
-- Name: file_versions file_versions_file_id_version_number_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.file_versions
    ADD CONSTRAINT file_versions_file_id_version_number_key UNIQUE (file_id, version_number);


--
-- TOC entry 4850 (class 2606 OID 16917)
-- Name: file_versions file_versions_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.file_versions
    ADD CONSTRAINT file_versions_pkey PRIMARY KEY (id);


--
-- TOC entry 4844 (class 2606 OID 16897)
-- Name: files files_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.files
    ADD CONSTRAINT files_pkey PRIMARY KEY (id);


--
-- TOC entry 4839 (class 2606 OID 16877)
-- Name: folders folders_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.folders
    ADD CONSTRAINT folders_pkey PRIMARY KEY (id);


--
-- TOC entry 4835 (class 2606 OID 16673)
-- Name: users users_email_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_email_key UNIQUE (email);


--
-- TOC entry 4837 (class 2606 OID 16671)
-- Name: users users_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);


--
-- TOC entry 4862 (class 1259 OID 16995)
-- Name: idx_activity_logs_created_at; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_activity_logs_created_at ON public.activity_logs USING btree (created_at);


--
-- TOC entry 4863 (class 1259 OID 16994)
-- Name: idx_activity_logs_user_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_activity_logs_user_id ON public.activity_logs USING btree (user_id);


--
-- TOC entry 4857 (class 1259 OID 16991)
-- Name: idx_file_permissions_file_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_file_permissions_file_id ON public.file_permissions USING btree (file_id);


--
-- TOC entry 4858 (class 1259 OID 16992)
-- Name: idx_file_permissions_folder_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_file_permissions_folder_id ON public.file_permissions USING btree (folder_id);


--
-- TOC entry 4859 (class 1259 OID 16993)
-- Name: idx_file_permissions_user_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_file_permissions_user_id ON public.file_permissions USING btree (user_id);


--
-- TOC entry 4845 (class 1259 OID 16989)
-- Name: idx_files_folder_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_files_folder_id ON public.files USING btree (folder_id);


--
-- TOC entry 4846 (class 1259 OID 16990)
-- Name: idx_files_owner_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_files_owner_id ON public.files USING btree (owner_id);


--
-- TOC entry 4840 (class 1259 OID 16986)
-- Name: idx_folders_owner_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_folders_owner_id ON public.folders USING btree (owner_id);


--
-- TOC entry 4841 (class 1259 OID 16985)
-- Name: idx_folders_parent_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_folders_parent_id ON public.folders USING btree (parent_id);


--
-- TOC entry 4842 (class 1259 OID 16987)
-- Name: idx_folders_path; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_folders_path ON public.folders USING gist (path);


--
-- TOC entry 4877 (class 2620 OID 16997)
-- Name: files update_users_modtime; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER update_users_modtime BEFORE UPDATE ON public.files FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();


--
-- TOC entry 4876 (class 2620 OID 16998)
-- Name: folders update_users_modtime; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER update_users_modtime BEFORE UPDATE ON public.folders FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();


--
-- TOC entry 4875 (class 2620 OID 16999)
-- Name: users update_users_modtime; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER update_users_modtime BEFORE UPDATE ON public.users FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();


--
-- TOC entry 4874 (class 2606 OID 16980)
-- Name: activity_logs activity_logs_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.activity_logs
    ADD CONSTRAINT activity_logs_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- TOC entry 4870 (class 2606 OID 16965)
-- Name: file_permissions file_permissions_created_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.file_permissions
    ADD CONSTRAINT file_permissions_created_by_fkey FOREIGN KEY (created_by) REFERENCES public.users(id);


--
-- TOC entry 4871 (class 2606 OID 16950)
-- Name: file_permissions file_permissions_file_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.file_permissions
    ADD CONSTRAINT file_permissions_file_id_fkey FOREIGN KEY (file_id) REFERENCES public.files(id);


--
-- TOC entry 4872 (class 2606 OID 16955)
-- Name: file_permissions file_permissions_folder_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.file_permissions
    ADD CONSTRAINT file_permissions_folder_id_fkey FOREIGN KEY (folder_id) REFERENCES public.folders(id);


--
-- TOC entry 4873 (class 2606 OID 16960)
-- Name: file_permissions file_permissions_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.file_permissions
    ADD CONSTRAINT file_permissions_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- TOC entry 4868 (class 2606 OID 16925)
-- Name: file_versions file_versions_created_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.file_versions
    ADD CONSTRAINT file_versions_created_by_fkey FOREIGN KEY (created_by) REFERENCES public.users(id);


--
-- TOC entry 4869 (class 2606 OID 16920)
-- Name: file_versions file_versions_file_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.file_versions
    ADD CONSTRAINT file_versions_file_id_fkey FOREIGN KEY (file_id) REFERENCES public.files(id);


--
-- TOC entry 4866 (class 2606 OID 16898)
-- Name: files files_folder_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.files
    ADD CONSTRAINT files_folder_id_fkey FOREIGN KEY (folder_id) REFERENCES public.folders(id);


--
-- TOC entry 4867 (class 2606 OID 16903)
-- Name: files files_owner_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.files
    ADD CONSTRAINT files_owner_id_fkey FOREIGN KEY (owner_id) REFERENCES public.users(id);


--
-- TOC entry 4864 (class 2606 OID 16883)
-- Name: folders folders_owner_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.folders
    ADD CONSTRAINT folders_owner_id_fkey FOREIGN KEY (owner_id) REFERENCES public.users(id);


--
-- TOC entry 4865 (class 2606 OID 16878)
-- Name: folders folders_parent_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.folders
    ADD CONSTRAINT folders_parent_id_fkey FOREIGN KEY (parent_id) REFERENCES public.folders(id);


-- Completed on 2025-02-06 10:12:37

--
-- PostgreSQL database dump complete
--

