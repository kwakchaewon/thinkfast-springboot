#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
테스트 데이터 생성 스크립트
test_data.sql 파일을 생성합니다.
"""

import uuid
import hashlib
import base64
import random
from datetime import datetime, timedelta
from typing import List, Dict

# BCrypt 해시 (비밀번호 "test1234"의 해시)
BCRYPT_HASH = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy"

# 현재 시간 기준
NOW = datetime.now()

def sha256_base64(text: str) -> str:
    """SHA256 해시 후 Base64 인코딩"""
    hash_obj = hashlib.sha256(text.encode('utf-8'))
    return base64.b64encode(hash_obj.digest()).decode('utf-8')

def generate_uuid() -> str:
    """UUID v4 생성"""
    return str(uuid.uuid4())

def format_datetime(dt: datetime) -> str:
    """MySQL DATETIME 형식으로 변환"""
    return dt.strftime('%Y-%m-%d %H:%M:%S')

def format_date(dt: datetime) -> str:
    """MySQL DATE 형식으로 변환"""
    return dt.strftime('%Y-%m-%d')

# ==================== 데이터 생성 ====================

# 사용자 데이터
users = []
# CREATOR 8명
for i in range(1, 9):
    birth_date = datetime(1980 + i*2, (i % 12) + 1, (i % 28) + 1)
    created_at = NOW - timedelta(days=365 - i*30)
    users.append({
        'username': f'test_creator_{i}@example.com',
        'password': BCRYPT_HASH,
        'birth_date': birth_date,
        'role': 'CREATOR',
        'created_at': created_at
    })

# ADMIN 2명
for i in range(1, 3):
    birth_date = datetime(1985 + i, (i % 12) + 1, (i % 28) + 1)
    created_at = NOW - timedelta(days=300 - i*30)
    users.append({
        'username': f'test_admin_{i}@example.com',
        'password': BCRYPT_HASH,
        'birth_date': birth_date,
        'role': 'ADMIN',
        'created_at': created_at
    })

# 설문 주제 목록
survey_topics = [
    ("서비스 만족도 조사", "고객 여러분의 소중한 의견을 듣고 싶습니다."),
    ("제품 피드백 수집", "새로운 제품에 대한 여러분의 생각을 알려주세요."),
    ("이벤트 참여 설문", "이벤트 참여 후기를 남겨주세요."),
    ("앱 사용성 조사", "앱 사용 경험을 공유해주세요."),
    ("고객 지원 만족도", "고객 지원 서비스에 대한 의견을 들려주세요."),
    ("브랜드 인지도 조사", "우리 브랜드에 대해 어떻게 생각하시나요?"),
    ("새 기능 테스트", "새로운 기능을 체험해보고 평가해주세요."),
    ("온라인 강의 만족도", "강의 콘텐츠에 대한 만족도를 평가해주세요."),
    ("온라인 쇼핑 경험", "쇼핑 경험을 공유해주세요."),
    ("웹사이트 개선 의견", "웹사이트 개선을 위한 의견을 주세요."),
]

# 질문 템플릿
multiple_choice_questions = [
    "현재 만족도는 어떠신가요?",
    "어떤 기능을 가장 자주 사용하시나요?",
    "주로 어떤 기기를 사용하시나요?",
    "어떤 서비스를 가장 선호하시나요?",
    "가장 중요하게 생각하는 것은 무엇인가요?",
    "어떤 방식으로 알림을 받고 싶으신가요?",
]

subjective_questions = [
    "개선하고 싶은 점이 있다면 무엇인가요?",
    "추가로 원하는 기능이 있다면 알려주세요.",
    "전반적인 의견이나 제안사항을 자유롭게 남겨주세요.",
    "가장 불편했던 점은 무엇이었나요?",
    "특히 좋았던 점은 무엇이었나요?",
    "더 나은 서비스를 위한 아이디어를 공유해주세요.",
]

multiple_choice_options = [
    ["매우 만족", "만족", "보통", "불만족", "매우 불만족"],
    ["기능 A", "기능 B", "기능 C", "기능 D"],
    ["스마트폰", "태블릿", "PC", "노트북"],
    ["서비스 1", "서비스 2", "서비스 3"],
    ["가격", "품질", "서비스", "편의성"],
    ["이메일", "SMS", "앱 푸시", "없음"],
]

# 응답 텍스트 (주관식용)
subjective_responses = [
    "전반적으로 만족스럽습니다.",
    "좀 더 빠른 응답이 필요합니다.",
    "UI/UX 개선이 필요해 보입니다.",
    "기능이 다양해서 좋습니다.",
    "가격대비 만족도가 높습니다.",
    "고객 지원이 친절합니다.",
    "추가 기능을 원합니다.",
    "안정성이 좋습니다.",
    "다양한 결제 수단이 필요합니다.",
    "모바일 앱 개선이 필요합니다.",
]

# SQL 생성 시작
sql_lines = []
sql_lines.append("-- =========================================")
sql_lines.append("-- 테스트 데이터 SQL 파일")
sql_lines.append("-- 생성 일시: " + format_datetime(NOW))
sql_lines.append("-- =========================================\n")

# USER 테이블 INSERT
sql_lines.append("-- =========================================")
sql_lines.append("-- USER 테이블 데이터")
sql_lines.append("-- =========================================\n")

for i, user in enumerate(users, 1):
    sql_lines.append(
        f"INSERT INTO `USER` (USERNAME, PASSWORD, BIRTH_DATE, ROLE, CREATED_AT, UPDATED_AT) "
        f"VALUES ("
        f"'{user['username']}', "
        f"'{user['password']}', "
        f"'{format_date(user['birth_date'])}', "
        f"'{user['role']}', "
        f"'{format_datetime(user['created_at'])}', "
        f"'{format_datetime(user['created_at'])}'"
        f"); -- User ID: {i}\n"
    )

sql_lines.append("\n-- USER ID 참조: 1-8 = CREATOR, 9-10 = ADMIN\n")

# 설문 데이터 생성
surveys = []
survey_id = 1
for creator_idx in range(1, 9):  # CREATOR 8명
    num_surveys = random.randint(3, 5)  # 각 CREATOR당 3-5개
    for s in range(num_surveys):
        topic_idx = (creator_idx + s) % len(survey_topics)
        title, description = survey_topics[topic_idx]
        
        # 설문 기간 설정
        days_ago = random.randint(0, 180)
        start_time = NOW - timedelta(days=days_ago)
        duration = random.randint(7, 90)
        end_time = start_time + timedelta(days=duration)
        
        # 설문 상태
        is_active = random.random() > 0.2  # 80% 활성
        is_deleted = random.random() > 0.9  # 10% 삭제됨
        
        created_at = start_time - timedelta(days=random.randint(1, 5))
        
        surveys.append({
            'id': survey_id,
            'user_id': creator_idx,
            'title': title + f" ({s+1})",
            'description': description,
            'start_time': start_time if start_time < NOW else None,
            'end_time': end_time,
            'is_active': is_active,
            'is_deleted': is_deleted,
            'created_at': created_at
        })
        survey_id += 1

# SURVEYS 테이블 INSERT
sql_lines.append("\n-- =========================================")
sql_lines.append("-- SURVEYS 테이블 데이터")
sql_lines.append("-- =========================================\n")

for survey in surveys:
    start_time_sql = f"'{format_datetime(survey['start_time'])}'" if survey['start_time'] else 'NULL'
    sql_lines.append(
        f"INSERT INTO SURVEYS (USER_ID, TITLE, DESCRIPTION, START_TIME, END_TIME, IS_ACTIVE, IS_DELETED, CREATED_AT, UPDATED_AT) "
        f"VALUES ("
        f"{survey['user_id']}, "
        f"'{survey['title']}', "
        f"'{survey['description']}', "
        f"{start_time_sql}, "
        f"'{format_datetime(survey['end_time'])}', "
        f"{1 if survey['is_active'] else 0}, "
        f"{1 if survey['is_deleted'] else 0}, "
        f"'{format_datetime(survey['created_at'])}', "
        f"'{format_datetime(survey['created_at'])}'"
        f"); -- Survey ID: {survey['id']}\n"
    )

sql_lines.append(f"\n-- 총 {len(surveys)}개 설문 생성 완료\n")

# 질문 데이터 생성
questions = []
question_id = 1
options = []
option_id = 1

for survey in surveys:
    if survey['is_deleted']:
        continue  # 삭제된 설문은 질문 없음
        
    num_questions = random.randint(3, 7)
    question_types = []
    
    # 질문 타입 분배 (MULTIPLE_CHOICE 50-60%, SUBJECTIVE 40-50%)
    num_mc = int(num_questions * random.uniform(0.5, 0.6))
    num_subjective = num_questions - num_mc
    
    question_types.extend(['MULTIPLE_CHOICE'] * num_mc)
    question_types.extend(['SUBJECTIVE'] * num_subjective)
    random.shuffle(question_types)
    
    for order_idx, q_type in enumerate(question_types, 1):
        if q_type == 'MULTIPLE_CHOICE':
            content = random.choice(multiple_choice_questions)
        else:
            content = random.choice(subjective_questions)
        
        questions.append({
            'id': question_id,
            'survey_id': survey['id'],
            'type': q_type,
            'content': content,
            'order_index': order_idx
        })
        
        # MULTIPLE_CHOICE인 경우 옵션 추가
        if q_type == 'MULTIPLE_CHOICE':
            option_set = random.choice(multiple_choice_options)
            for opt_content in option_set:
                options.append({
                    'id': option_id,
                    'question_id': question_id,
                    'content': opt_content
                })
                option_id += 1
        
        question_id += 1

# QUESTIONS 테이블 INSERT
sql_lines.append("\n-- =========================================")
sql_lines.append("-- QUESTIONS 테이블 데이터")
sql_lines.append("-- =========================================\n")

for question in questions:
    sql_lines.append(
        f"INSERT INTO QUESTIONS (SURVEY_ID, TYPE, CONTENT, ORDER_INDEX) "
        f"VALUES ("
        f"{question['survey_id']}, "
        f"'{question['type']}', "
        f"'{question['content'].replace("'", "''")}', "
        f"{question['order_index']}"
        f"); -- Question ID: {question['id']}\n"
    )

sql_lines.append(f"\n-- 총 {len(questions)}개 질문 생성 완료\n")

# OPTIONS 테이블 INSERT
sql_lines.append("\n-- =========================================")
sql_lines.append("-- OPTIONS 테이블 데이터")
sql_lines.append("-- =========================================\n")

for opt in options:
    sql_lines.append(
        f"INSERT INTO OPTIONS (QUESTION_ID, CONTENT) "
        f"VALUES ("
        f"{opt['question_id']}, "
        f"'{opt['content'].replace("'", "''")}'"
        f"); -- Option ID: {opt['id']}\n"
    )

sql_lines.append(f"\n-- 총 {len(options)}개 옵션 생성 완료\n")

# 응답 데이터 생성
responses = []
response_histories = []

# 설문별 질문 맵 생성
survey_questions_map = {}
for q in questions:
    if q['survey_id'] not in survey_questions_map:
        survey_questions_map[q['survey_id']] = []
    survey_questions_map[q['survey_id']].append(q)

# 질문별 옵션 맵 생성
question_options_map = {}
for opt in options:
    if opt['question_id'] not in question_options_map:
        question_options_map[opt['question_id']] = []
    question_options_map[opt['question_id']].append(opt)

response_id = 1
for survey in surveys:
    if survey['is_deleted']:
        continue
    
    # 설문별 응답 개수 결정
    if survey['is_active'] and survey['end_time'] > NOW:
        num_responses = random.randint(30, 50)  # 활성 설문
    elif survey['end_time'] < NOW:
        num_responses = random.randint(15, 30)  # 종료된 설문
    else:
        num_responses = random.randint(5, 15)  # 비활성 설문
    
    if survey['id'] not in survey_questions_map:
        continue
    
    survey_questions = survey_questions_map[survey['id']]
    
    for response_session in range(num_responses):
        session_id = generate_uuid()
        response_time = survey['start_time'] if survey['start_time'] else survey['created_at']
        if survey['end_time'] > response_time:
            response_time = response_time + timedelta(
                seconds=random.randint(0, int((survey['end_time'] - response_time).total_seconds()))
            )
        else:
            response_time = survey['created_at']
        
        # 각 질문에 응답 생성
        answered_questions = random.sample(
            survey_questions,
            random.randint(max(1, len(survey_questions) - 2), len(survey_questions))
        )
        
        for question in answered_questions:
            if question['type'] == 'MULTIPLE_CHOICE':
                question_options = question_options_map.get(question['id'], [])
                if question_options:
                    selected_option = random.choice(question_options)
                    responses.append({
                        'id': response_id,
                        'response_session_id': session_id,
                        'question_id': question['id'],
                        'question_type': 'MULTIPLE_CHOICE',
                        'option_id': selected_option['id'],
                        'subjective_content': None,
                        'scale_value': None,
                        'created_at': response_time
                    })
            else:  # SUBJECTIVE
                responses.append({
                    'id': response_id,
                    'response_session_id': session_id,
                    'question_id': question['id'],
                    'question_type': 'SUBJECTIVE',
                    'option_id': None,
                    'subjective_content': random.choice(subjective_responses),
                    'scale_value': None,
                    'created_at': response_time
                })
            response_id += 1
        
        # 응답 이력 생성
        device_id = f"device_{survey['id']}_{response_session}_{random.randint(1000, 9999)}"
        ip_address = f"192.168.{random.randint(1, 255)}.{random.randint(1, 255)}"
        
        response_histories.append({
            'survey_id': survey['id'],
            'device_id_hash': sha256_base64(device_id),
            'ip_address_hash': sha256_base64(ip_address),
            'responded_at': response_time
        })

# RESPONSES 테이블 INSERT
sql_lines.append("\n-- =========================================")
sql_lines.append("-- RESPONSES 테이블 데이터")
sql_lines.append("-- =========================================\n")

for response in responses:
    option_id_sql = str(response['option_id']) if response['option_id'] else 'NULL'
    subjective_sql = f"'{response['subjective_content'].replace("'", "''")}'" if response['subjective_content'] else 'NULL'
    scale_value_sql = str(response['scale_value']) if response['scale_value'] else 'NULL'
    
    sql_lines.append(
        f"INSERT INTO RESPONSES (RESPONSE_SESSION_ID, QUESTION_ID, QUESTION_TYPE, OPTION_ID, SUBJECTIVE_CONTENT, SCALE_VALUE, CREATED_AT) "
        f"VALUES ("
        f"'{response['response_session_id']}', "
        f"{response['question_id']}, "
        f"'{response['question_type']}', "
        f"{option_id_sql}, "
        f"{subjective_sql}, "
        f"{scale_value_sql}, "
        f"'{format_datetime(response['created_at'])}'"
        f"); -- Response ID: {response['id']}\n"
    )

sql_lines.append(f"\n-- 총 {len(responses)}개 응답 생성 완료\n")

# SURVEY_RESPONSE_HISTORY 테이블 INSERT
sql_lines.append("\n-- =========================================")
sql_lines.append("-- SURVEY_RESPONSE_HISTORY 테이블 데이터")
sql_lines.append("-- =========================================\n")

for history in response_histories:
    sql_lines.append(
        f"INSERT INTO SURVEY_RESPONSE_HISTORY (SURVEY_ID, DEVICE_ID_HASH, IP_ADDRESS_HASH, RESPONDED_AT) "
        f"VALUES ("
        f"{history['survey_id']}, "
        f"'{history['device_id_hash']}', "
        f"'{history['ip_address_hash']}', "
        f"'{format_datetime(history['responded_at'])}'"
        f");\n"
    )

sql_lines.append(f"\n-- 총 {len(response_histories)}개 응답 이력 생성 완료\n")

# 파일 저장
with open('test_data.sql', 'w', encoding='utf-8') as f:
    f.write('\n'.join(sql_lines))

print(f"✅ test_data.sql 파일 생성 완료!")
print(f"   - 사용자: {len(users)}명")
print(f"   - 설문: {len(surveys)}개")
print(f"   - 질문: {len(questions)}개")
print(f"   - 옵션: {len(options)}개")
print(f"   - 응답: {len(responses)}개")
print(f"   - 응답 이력: {len(response_histories)}개")
print(f"   - 총 INSERT 문: {len(users) + len(surveys) + len(questions) + len(options) + len(responses) + len(response_histories)}개")

