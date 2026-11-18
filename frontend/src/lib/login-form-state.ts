/** 로그인 폼을 제출한 뒤 서버 액션이 돌려주는 상태. 비밀번호는 담지 않는다. */
export type LoginFormState = {
  username: string;
  message?: string;
};
