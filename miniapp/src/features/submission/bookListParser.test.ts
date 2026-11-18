
import { describe, expect, it } from "vitest";
import {
  parseBookList,
  normalizeBookTitle,
} from "./bookListParser";
import { validateBookList } from "./bookListValidator";

describe("parseBookList", () => {
  it("parses numbered books", () => {
    expect(parseBookList("1. Dune\n2. 1984")).toEqual([
      "Dune",
      "1984",
    ]);
  });

  it("parses different list markers", () => {
    expect(parseBookList("- Dune\n• 1984\n3) Hamlet")).toEqual([
      "Dune",
      "1984",
      "Hamlet",
    ]);
  });

  it("ignores empty lines", () => {
    expect(parseBookList("Dune\n\n  \n1984")).toEqual([
      "Dune",
      "1984",
    ]);
  });

  it("keeps commas and dashes inside titles", () => {
    expect(parseBookList("Love, Death & Robots\n" +
      "Flowers for Algernon — Daniel Keyes")).toEqual([
      "Love, Death & Robots",
      "Flowers for Algernon — Daniel Keyes",
    ]);
  });
});

describe("validateBookList", () => {
  const tenBooks = Array.from(
    { length: 10 },
    (_, index) => `Book ${index + 1}`
  );

  it("accepts exactly ten unique books", () => {
    expect(validateBookList(tenBooks).isValid).toBe(true);
  });

  it("rejects fewer than ten books", () => {
    expect(validateBookList(tenBooks.slice(0, 9)).isValid)
      .toBe(false);
  });

  it("rejects more than ten books", () => {
    expect(validateBookList([...tenBooks, "Book 11"]).isValid)
      .toBe(false);
  });

  it("detects duplicates regardless of case and spaces", () => {
    const books = [
      ...tenBooks.slice(0, 9),
      "  BOOK 1  ",
    ];

    expect(validateBookList(books).isValid).toBe(false);
  });

  it("detects empty titles", () => {
    const books = [...tenBooks.slice(0, 9), ""];

    expect(validateBookList(books).hasEmptyBooks).toBe(true);
    expect(validateBookList(books).isValid).toBe(false);
  });
});

describe("normalizeBookTitle", () => {
  it("normalizes case and whitespace", () => {
    expect(normalizeBookTitle("  The   Book  "))
      .toBe("the book");
  });
});
