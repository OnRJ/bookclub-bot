import { submitBooks } from "./features/submission/submissionApi";
import { useMemo, useState } from "react";
import { parseBookList } from "./features/submission/bookListParser";
import {
  MAX_BOOKS,
  validateBookList,
} from "./features/submission/bookListValidator";
import "./App.css";

function App() {
  console.log("Telegram:", window.Telegram?.WebApp);
  console.log("InitData:", window.Telegram?.WebApp?.initData);
  console.log(
    "StartParam:",
    window.Telegram?.WebApp?.initDataUnsafe?.start_param
  );

  const [input, setInput] = useState("");
  const [books, setBooks] = useState<string[]>([]);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [submitSuccess, setSubmitSuccess] = useState<string | null>(null);

  const validation = useMemo(
    () => validateBookList(books),
    [books]
  );

  const handleInputChange = (value: string) => {
    setInput(value);
    setBooks(parseBookList(value));
  };

  const updateBook = (index: number, value: string) => {
    setBooks((current) =>
      current.map((book, i) => (i === index ? value : book))
    );
  };

  const removeBook = (index: number) => {
    setBooks((current) =>
      current.filter((_, i) => i !== index)
    );
  };

  const addBook = () => {
    if (books.length >= MAX_BOOKS) return;
    setBooks((current) => [...current, ""]);
  };

const handleSubmit = async () => {
  if (!validation.isValid || isSubmitting) return;

  setIsSubmitting(true);
  setSubmitError(null);
  setSubmitSuccess(null);

  try {
    const result = await submitBooks(books);

    setSubmitSuccess(
      `${result.message}.`
    );
  } catch (error) {
    setSubmitError(
      error instanceof Error
        ? error.message
        : "Не удалось отправить список."
    );
  } finally {
    setIsSubmitting(false);
  }
};

  return (
    <main className="app">
      <header className="header">
        <div className="logo">BOOK CLUB</div>
        <div className="month">BOOK SUBMISSION</div>
      </header>

      <section className="intro">
        <div className="eyebrow">НОВЫЙ МЕСЯЦ · НОВЫЙ СПИСОК</div>
        <h1>Предложи книги<br />для клуба</h1>
        <p>
          Скопируй и вставь сюда свой список из 10 книг ❤️
        </p>
      </section>

      <section className="input-section">
        <div className="section-heading">
          <label htmlFor="book-list">Твой список</label>
          <span>10 книг</span>
        </div>

        <textarea
          id="book-list"
          value={input}
          onChange={(event) => handleInputChange(event.target.value)}
          placeholder={
            "Вставь сюда список из заметок...\n\n" +
            "1. Мастер и Маргарита\n" +
            "2. 1984\n" +
            "3. Цветы для Элджернона"
          }
          rows={8}
        />

        <div className="input-hint">
          Каждую книгу указывай с новой строки.
        </div>
      </section>

      <section className="preview-section">
        <div className="section-heading">
          <label>Твои книги</label>
          <span className={validation.isValid ? "count complete" : "count"}>
            {validation.count} / {MAX_BOOKS}
          </span>
        </div>

        <div className="progress-track">
          <div
            className="progress-value"
            style={{
              width: `${Math.min(validation.count / MAX_BOOKS, 1) * 100}%`,
            }}
          />
        </div>

        {books.length === 0 ? (
          <div className="empty-state">
            Здесь появятся твои книги
          </div>
        ) : (
          <div className="book-list">
            {books.map((book, index) => {
              const isDuplicate =
                validation.duplicateIndexes.includes(index);

              return (
                <div className="book-item" key={index}>
                  <span className="book-number">{index + 1}</span>

                  <input
                    value={book}
                    onChange={(event) =>
                      updateBook(index, event.target.value)
                    }
                    placeholder="Название книги"
                    aria-label={`Книга ${index + 1}`}
                  />

                  <button
                    type="button"
                    className="remove-book"
                    onClick={() => removeBook(index)}
                    aria-label={`Удалить книгу ${index + 1}`}
                    title="Удалить книгу"
                  >
                    ×
                  </button>

                  {isDuplicate && (
                    <span className="duplicate-indicator">!</span>
                  )}
                </div>
              );
            })}
          </div>
        )}

        {books.length < MAX_BOOKS && (
          <button
            type="button"
            className="add-book-button"
            onClick={addBook}
          >
            + Добавить книгу
          </button>
        )}

        {validation.hasTooFewBooks && books.length > 0 && (
          <p className="helper-message">
            Осталось добавить: {validation.remaining}
          </p>
        )}

        {validation.hasTooManyBooks && (
          <p className="error-message">
            В списке больше 10 книг. Удали лишние.
          </p>
        )}

        {validation.duplicateIndexes.length > 0 && (
          <p className="error-message">
            Есть повторяющиеся названия. Проверь отмеченные строки.
          </p>
        )}

        {validation.hasEmptyBooks && (
          <p className="error-message">
            Заполни пустые строки или удали их.
          </p>
        )}

        {validation.isValid && (
          <p className="success-message">
            Отлично! Все 10 книг готовы ❤️
          </p>
        )}
      </section>

    <button
      className="submit-button"
      disabled={!validation.isValid || isSubmitting || !!submitSuccess}
      onClick={handleSubmit}
    >
      {isSubmitting
        ? "Отправляем..."
        : submitSuccess
          ? "Список отправлен ❤️"
          : "Отправить список"}
    </button>

    {submitError && (
      <p className="error-message" role="alert">
        {submitError}
      </p>
    )}

    {submitSuccess && (
      <p className="success-message" role="status">
        {submitSuccess}
      </p>
    )}

      <footer>BOOK CLUB · READING TOGETHER</footer>
    </main>
  );
}

export default App;
