
import { useState } from 'react';
import { apiUrl } from './api';
import { DISTRICTS } from './districts';
import './AuthModal.css';

const initialSignupForm = {
  email: '',
  password: '',
  name: '',
  region: 'SEOUL',
  district: '용산구',
  preferredSeat: 'NO_PREFERENCE',
  ageGroup: 'ADULT',
};

function EyeIcon({ hidden }) {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true">
      <path
        d="M2 12s3.5-6 10-6 10 6 10 6-3.5 6-10 6S2 12 2 12z"
        fill="none"
        stroke="currentColor"
        strokeWidth="1.8"
      />
      <circle
        cx="12"
        cy="12"
        r="2.5"
        fill="none"
        stroke="currentColor"
        strokeWidth="1.8"
      />
      {hidden && (
        <path
          d="M5 5l14 14"
          fill="none"
          stroke="currentColor"
          strokeWidth="1.8"
          strokeLinecap="round"
        />
      )}
    </svg>
  )
}

async function readResponse(response) {
  const text = await response.text();

  if (!text) {
    return {};
  }

  try {
    return JSON.parse(text);
  } catch {
    return { message: text };
  }
}

function getErrorMessage(data, fallback) {
  if (typeof data.message === 'string') {
    return data.message;
  }

  if (typeof data.error === 'string') {
    return data.error;
  }

  return fallback;
}

export default function AuthModal({ onClose, onLogin, onSignup }) {
  const [mode, setMode] = useState('login');
  const [loginForm, setLoginForm] = useState({
    email: '',
    password: '',
  });
  const [signupForm, setSignupForm] = useState(initialSignupForm);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [passwordVisible, setPasswordVisible] = useState(false);

  const isSignup = mode === 'signup';

  function changeMode(nextMode) {
    setMode(nextMode);
    setMessage('');
    setError('');
    setPasswordVisible(false);
  }

  function handleLoginChange(event) {
    const { name, value } = event.target;

    setLoginForm((prev) => ({
      ...prev,
      [name]: value,
    }));
  }

  function handleSignupChange(event) {
    const { name, value } = event.target;

    setSignupForm((prev) => ({
      ...prev,
      [name]: value,
      ...(name === 'region'
        ? { district: (DISTRICTS[value] || []).includes(prev.district) ? prev.district : DISTRICTS[value][0] }
        : {}),
    }));
  }

  async function handleSubmit(event) {
    event.preventDefault();
    setMessage('');
    setError('');
    setLoading(true);

    try {
      if (isSignup) {
        const response = await fetch(apiUrl('/api/users/signup'), {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
          },
          body: JSON.stringify(signupForm),
        });

        const data = await readResponse(response);

        if (!response.ok) {
          throw new Error(
            getErrorMessage(data, '회원가입에 실패했습니다.')
          );
        }

        setLoginForm({
          email: signupForm.email,
          password: '',
        });

        const signedUpEmail = signupForm.email;
        setSignupForm(initialSignupForm);
        setMode('login');
        setMessage('회원가입이 완료됐어요. 로그인해 주세요.');
        onSignup?.(signedUpEmail);
        return;
      }

      const response = await fetch(apiUrl('/api/users/login'), {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(loginForm),
      });

      const data = await readResponse(response);

      if (!response.ok) {
        throw new Error(
          getErrorMessage(data, '이메일 또는 비밀번호를 확인해 주세요.')
        );
      }

      if (!data.accessToken) {
        throw new Error('로그인 토큰을 받지 못했습니다.');
      }

      const meResponse = await fetch(apiUrl('/api/users/me'), {
        headers: {
          Authorization: `Bearer ${data.accessToken}`,
        },
      });

      const user = await readResponse(meResponse);

      if (!meResponse.ok) {
        throw new Error(
          getErrorMessage(user, '로그인 사용자 정보를 확인하지 못했습니다.')
        );
      }

      sessionStorage.setItem('accessToken', data.accessToken);

      onLogin({
        accessToken: data.accessToken,
        user,
      });

      onClose();
    } catch (err) {
      setError(err.message || '요청 처리 중 오류가 발생했습니다.');
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="auth-overlay">
      <section
        className="auth-modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="auth-title"
      >
        <button
          type="button"
          className="auth-close"
          onClick={onClose}
          aria-label="닫기"
        >
          ×
        </button>

        <p className="auth-eyebrow">CINEQUEUE MEMBERS</p>
        <h2 id="auth-title">
          {isSignup ? '회원가입' : '다시 만나서 반가워요'}
        </h2>
        <p className="auth-description">
          {isSignup
            ? '계정을 만들고 편리하게 영화를 예매해 보세요.'
            : '로그인하고 나만의 영화 예매를 시작하세요.'}
        </p>

        <div className="auth-tabs">
          <button
            type="button"
            className={!isSignup ? 'active' : ''}
            onClick={() => changeMode('login')}
          >
            로그인
          </button>
          <button
            type="button"
            className={isSignup ? 'active' : ''}
            onClick={() => changeMode('signup')}
          >
            회원가입
          </button>
        </div>

        <form className="auth-form" onSubmit={handleSubmit}>
          {isSignup && (
            <label>
              이름
              <input
                name="name"
                value={signupForm.name}
                onChange={handleSignupChange}
                placeholder="이름을 입력해 주세요"
                autoComplete="name"
                required
              />
            </label>
          )}

          <label>
            이메일
            <input
              type="email"
              name="email"
              value={isSignup ? signupForm.email : loginForm.email}
              onChange={isSignup ? handleSignupChange : handleLoginChange}
              placeholder="name@example.com"
              autoComplete="email"
              required
            />
          </label>

          <label>
            비밀번호
            <span className="password-field">
              <input
                type={passwordVisible ? 'text' : 'password'}
                name="password"
                value={isSignup ? signupForm.password : loginForm.password}
                onChange={isSignup ? handleSignupChange : handleLoginChange}
                placeholder={
                  isSignup ? '8자 이상 입력해 주세요' : '비밀번호를 입력해 주세요'
                }
                autoComplete={isSignup ? 'new-password' : 'current-password'}
                minLength={isSignup ? 8 : undefined}
                required
              />
              <button
                type="button"
                className="password-toggle"
                onClick={() => setPasswordVisible((visible) => !visible)}
                aria-label={passwordVisible ? '비밀번호 숨기기' : '비밀번호 표시'}
                aria-pressed={passwordVisible}
              >
                <EyeIcon hidden={passwordVisible} />
              </button>
            </span>
          </label>

          {isSignup && (
            <>
              <label>
                나이대
                <select
                  name="ageGroup"
                  value={signupForm.ageGroup}
                  onChange={handleSignupChange}
                  required
                >
                  <option value="CHILD">유아</option>
                  <option value="TEEN">청소년</option>
                  <option value="ADULT">성인</option>
                </select>
              </label>

              <label>
                지역
                <select
                  name="region"
                  value={signupForm.region}
                  onChange={handleSignupChange}
                  required
                >
                  <option value="SEOUL">서울</option>
                  <option value="GYEONGGI">경기</option>
                  <option value="INCHEON">인천</option>
                  <option value="GANGWON">강원</option>
                  <option value="CHUNGCHEONG">충청</option>
                  <option value="JEOLLA">전라</option>
                  <option value="GYEONGSANG">경상</option>
                  <option value="JEJU">제주</option>
                  <option value="OTHER">기타</option>
                </select>
              </label>

              <label>
                시·군·구
                <select
                  name="district"
                  value={signupForm.district}
                  onChange={handleSignupChange}
                  required
                >
                  {(DISTRICTS[signupForm.region] || []).map((district) => (
                    <option key={district} value={district}>{district}</option>
                  ))}
                </select>
              </label>

              <label>
                선호 좌석
                <select
                  name="preferredSeat"
                  value={signupForm.preferredSeat}
                  onChange={handleSignupChange}
                  required
                >
                  <option value="NO_PREFERENCE">선호 없음</option>
                  <option value="FRONT">앞쪽</option>
                  <option value="MIDDLE">중간</option>
                  <option value="BACK">뒤쪽</option>
                  <option value="AISLE">통로</option>
                  <option value="CENTER">중앙</option>
                </select>
              </label>
            </>
          )}

          {message && (
            <p className="auth-message" role="status">
              {message}
            </p>
          )}

          {error && (
            <p className="auth-error" role="alert">
              {error}
            </p>
          )}

          <button
            type="submit"
            className="auth-submit"
            disabled={loading}
          >
            {loading
              ? '처리 중...'
              : isSignup
                ? '계정 만들기'
                : '로그인'}
          </button>
        </form>

        <p className="auth-footer">
          CineQueue와 함께 오늘의 영화를 찾아보세요.
        </p>
      </section>
    </div>
  );
}