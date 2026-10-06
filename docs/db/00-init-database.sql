-- ============================================
-- Carlos Framework PostgreSQL 数据库初始化
-- 生成日期: 2026-05-28
-- 数据库: PostgreSQL 14+
-- 字符集: UTF8
-- ============================================

-- 创建数据库 (以 postgres 用户执行)
CREATE DATABASE carlos_framework WITH OWNER = postgres ENCODING = 'UTF8' LC_COLLATE = 'zh_CN.UTF-8' LC_CTYPE = 'zh_CN.UTF-8' TEMPLATE = template0;

-- 连接数据库
-- \c carlos;

-- 创建 schema
CREATE SCHEMA IF NOT EXISTS public;

-- 设置时区
SET TIMEZONE = 'Asia/Shanghai';
