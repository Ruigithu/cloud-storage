export const isEmpty = (inputValue) => !inputValue || !inputValue.trim();

export const isEmailFormat = (email) => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);

export const isMinLength = (inputValue, length) => inputValue.length >= length;

