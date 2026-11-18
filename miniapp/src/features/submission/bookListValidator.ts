
import { normalizeBookTitle } from "./bookListParser";

export const MAX_BOOKS = 10;

export type BookListValidation = {
  count: number;
  remaining: number;
  hasTooManyBooks: boolean;
  hasTooFewBooks: boolean;
  hasEmptyBooks: boolean;
  duplicateIndexes: number[];
  isValid: boolean;
};

export function validateBookList(
  books: string[]
): BookListValidation {
  const normalized = books.map(normalizeBookTitle);

  const duplicateIndexes = normalized.reduce<number[]>(
    (duplicates, title, index) => {
      if (
        title !== "" &&
        normalized.indexOf(title) !== index
      ) {
        duplicates.push(index);
      }

      return duplicates;
    },
    []
  );

  const count = books.length;
  const hasEmptyBooks = books.some((book) => !book.trim());

  const hasTooManyBooks = count > MAX_BOOKS;
  const hasTooFewBooks = count < MAX_BOOKS;

  return {
    count,
    remaining: Math.max(0, MAX_BOOKS - count),
    hasTooManyBooks,
    hasTooFewBooks,
    hasEmptyBooks,
    duplicateIndexes,
    isValid:
      count === MAX_BOOKS &&
      !hasEmptyBooks &&
      duplicateIndexes.length === 0,
  };
}
