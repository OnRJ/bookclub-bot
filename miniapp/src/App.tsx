import { BookListInput } from "./features/submission/BookListInput";
import { BookListPreview } from "./features/submission/BookListPreview";
import { SubmissionStatus } from "./features/submission/SubmissionStatus";
import { useBookSubmission } from "./features/submission/useBookSubmission";
import "./App.css";

function App() {
  const submission = useBookSubmission();
  const { input, books, validation, isSubmitting, submitError, submitSuccess } = submission;

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

      <BookListInput value={input} onChange={submission.updateInput} />
      <BookListPreview books={books} validation={validation} onUpdate={submission.updateBook} onRemove={submission.removeBook} onAdd={submission.addBook} />

    <button
      className="submit-button"
      disabled={!validation.isValid || isSubmitting || !!submitSuccess}
      onClick={submission.submit}
    >
      {isSubmitting
        ? "Отправляем..."
        : submitSuccess
          ? "Список отправлен ❤️"
          : "Отправить список"}
    </button>

    <SubmissionStatus error={submitError} success={submitSuccess} />

      <footer>BOOK CLUB · READING TOGETHER</footer>
    </main>
  );
}

export default App;
