-- =====================================================================
-- Oracle Autonomous DB (Always Free) 에서 ADMIN 으로 실행
--   EBSDEMO : 데모 테이블 소유자 (데이터 초기화 작업 실행)
--   EBSAPP  : 웹 애플리케이션 접속용, SELECT 권한만
-- 실행 전에 두 비밀번호를 반드시 바꾸세요 (12자 이상, 대·소문자·숫자 포함, 큰따옴표 안 문자 그대로)
-- =====================================================================
CREATE USER ebsdemo IDENTIFIED BY "Change_Me_Owner_2026" QUOTA 1G ON data;
GRANT CREATE SESSION, CREATE TABLE, CREATE SEQUENCE, CREATE PROCEDURE, CREATE JOB TO ebsdemo;

CREATE USER ebsapp IDENTIFIED BY "Change_Me_App_2026";
GRANT CREATE SESSION TO ebsapp;
