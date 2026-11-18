const API_URL = import.meta.env.VITE_API_BASE_URL || "";

interface SubmitBooksResponse {
  submissionId: number;
  readingMonth: string;
  bookCount: number;
  message: string;
}

interface ApiError {
  message?: string;
}

export async function submitBooks(
  bookTitles: string[]
): Promise<SubmitBooksResponse> {
  const initData = window.Telegram?.WebApp?.initData;

  if (!initData) {
    throw new Error(
      "Открой приложение через Telegram, чтобы отправить список."
    );
  }

  const response = await fetch(`${API_URL}/api/submissions`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      initData,
      bookTitles,
    }),
  });

  const data = await response.json();

  if (!response.ok) {
    const error = data as ApiError;

    throw new Error(
      error.message ?? "Не удалось отправить список книг."
    );
  }

  return data as SubmitBooksResponse;
}