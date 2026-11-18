/** 글 작성·수정 폼의 입력값. */
export type PostFormValues = {
  title: string;
  content: string;
  /** 입력칸에 적힌 그대로의 문자열 (예: "spring, jpa"). 서버 액션에서 쉼표로 나눈다. */
  tags: string;
};

/**
 * 폼을 제출한 뒤 서버 액션이 돌려주는 상태.
 * 저장에 실패하면 사용자가 쓴 내용(values)을 그대로 돌려주어 다시 입력하지 않게 한다.
 */
export type PostFormState = {
  values: PostFormValues;
  /** 필드별 검증 오류. 해당 입력칸 아래에 보여준다. */
  fieldErrors: Partial<Record<keyof PostFormValues, string>>;
  /** 특정 필드와 무관한 오류(서버 연결 실패 등). 폼 맨 위에 보여준다. */
  message?: string;
  /**
   * 로그인이 풀려서 저장하지 못한 경우. 로그인 화면으로 보내 버리면 쓰던 글이 사라지므로,
   * 화면은 그대로 두고 새 탭에서 로그인할 수 있는 링크를 보여준다.
   */
  loginRequired?: boolean;
};
