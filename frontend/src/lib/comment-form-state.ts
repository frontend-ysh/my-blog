/** 댓글 작성 폼을 제출한 뒤 서버 액션이 돌려주는 상태. 비밀번호는 담지 않는다. */
export type CommentFormState = {
  values: {
    authorName: string;
    content: string;
  };
  fieldErrors: {
    authorName?: string;
    password?: string;
    content?: string;
  };
  /** 특정 입력칸과 무관한 오류 */
  message?: string;
};

/** 댓글 삭제 폼의 상태. */
export type CommentDeleteState = {
  message?: string;
};
