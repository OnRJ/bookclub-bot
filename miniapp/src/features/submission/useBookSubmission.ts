import { useMemo, useState } from "react";
import { parseBookList } from "./bookListParser";
import { validateBookList } from "./bookListValidator";
import { submitBooks } from "./submissionApi";

export function useBookSubmission() {
  const [input, setInput] = useState("");
  const [books, setBooks] = useState<string[]>([]);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [submitSuccess, setSubmitSuccess] = useState<string | null>(null);
  const validation = useMemo(() => validateBookList(books), [books]);

  const updateInput = (value: string) => {
    setInput(value);
    setBooks(parseBookList(value));
  };

  const updateBook = (index: number, value: string) =>
    setBooks((current) => current.map((book, i) => i === index ? value : book));

  const removeBook = (index: number) =>
    setBooks((current) => current.filter((_, i) => i !== index));

  const addBook = () => {
    if (books.length < 10) setBooks((current) => [...current, ""]);
  };

  const submit = async () => {
    if (!validation.isValid || isSubmitting) return;
    setIsSubmitting(true);
    setSubmitError(null);
    setSubmitSuccess(null);
    try {
      const result = await submitBooks(books);
      setSubmitSuccess(result.message);
    } catch (error) {
      setSubmitError(error instanceof Error ? error.message : "Не удалось отправить список.");
    } finally {
      setIsSubmitting(false);
    }
  };

  return { input, books, validation, isSubmitting, submitError, submitSuccess,
    updateInput, updateBook, removeBook, addBook, submit };
}
