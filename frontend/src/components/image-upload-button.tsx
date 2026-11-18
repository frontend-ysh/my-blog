"use client";

import { type ChangeEvent, useRef, useState } from "react";
import { uploadImage } from "@/app/posts/image-actions";

const MAX_BYTES = 5 * 1024 * 1024;

/**
 * "이미지 첨부" 버튼. 파일을 고르면 바로 올리고, 받은 주소를 마크다운 이미지 문법으로
 * 본문 입력칸의 커서 자리에 넣는다.
 *
 * @param targetId 본문 입력칸(textarea)의 id
 */
export function ImageUploadButton({ targetId }: { targetId: string }) {
  const inputRef = useRef<HTMLInputElement>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string>();

  async function handleChange(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0];
    // 값을 비워 두어야 같은 파일을 다시 골라도 change 이벤트가 난다.
    event.target.value = "";
    if (!file) {
      return;
    }

    setError(undefined);
    // 큰 파일은 서버로 보내 보기 전에 여기서 걸러 낸다. (서버도 같은 검사를 다시 한다.)
    if (file.size > MAX_BYTES) {
      setError("이미지는 5MB 이하여야 합니다.");
      return;
    }

    setBusy(true);
    try {
      const formData = new FormData();
      formData.set("file", file);
      const result = await uploadImage(formData);
      if ("error" in result) {
        setError(result.error);
        return;
      }
      insertAtCursor(targetId, `![${altText(file.name)}](${result.url})`);
    } catch {
      setError("이미지를 올리지 못했습니다. 잠시 후 다시 시도해 주세요.");
    } finally {
      setBusy(false);
    }
  }

  return (
    <span className="flex items-center gap-2">
      {error && (
        <span role="alert" className="text-xs text-red-600 dark:text-red-400">
          {error}
        </span>
      )}
      {/* name이 없는 입력칸은 폼을 제출할 때 함께 보내지지 않는다. 파일은 위에서 따로 올린다. */}
      <input
        ref={inputRef}
        type="file"
        accept="image/png,image/jpeg,image/gif,image/webp"
        onChange={handleChange}
        hidden
        aria-label="첨부할 이미지 파일"
      />
      <button
        type="button"
        onClick={() => inputRef.current?.click()}
        disabled={busy}
        className="rounded-md border border-zinc-300 px-2.5 py-1 text-xs hover:bg-zinc-100 disabled:opacity-50 dark:border-zinc-700 dark:hover:bg-zinc-900"
      >
        {busy ? "올리는 중…" : "이미지 첨부"}
      </button>
    </span>
  );
}

/** 입력칸의 커서 자리(선택한 글자가 있으면 그 자리)에 글을 넣는다. */
function insertAtCursor(textareaId: string, markdown: string) {
  const textarea = document.getElementById(textareaId);
  if (!(textarea instanceof HTMLTextAreaElement)) {
    return;
  }

  const before = textarea.value.slice(0, textarea.selectionStart);
  const after = textarea.value.slice(textarea.selectionEnd);
  // 마크다운에서 이미지가 앞뒤 글과 한 문단으로 붙지 않도록 빈 줄로 띄운다.
  const prefix = before === "" || before.endsWith("\n\n") ? "" : before.endsWith("\n") ? "\n" : "\n\n";
  const suffix = after.startsWith("\n\n") ? "" : after.startsWith("\n") ? "\n" : "\n\n";

  textarea.setRangeText(
    prefix + markdown + suffix,
    textarea.selectionStart,
    textarea.selectionEnd,
    "end",
  );
  textarea.focus();
}

/** 파일 이름에서 확장자와 마크다운 문법에 쓰이는 괄호를 떼어 이미지 설명(대체 텍스트)으로 쓴다. */
function altText(filename: string): string {
  const name = filename.replace(/\.[^.]+$/, "").replace(/[[\]()]/g, " ").trim();
  return name || "이미지";
}
