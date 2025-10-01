import {isEmpty,isEmailFormat,isMinLength} from "./utils";

test("test isEmpty()",()=>{
    expect(isEmpty("")).toBe(true);
    expect(isEmpty(" ")).toBe(true);
    expect(isEmpty("false")).toBe(false);
});

test("test isEmailFormat",()=>{
    expect(isEmailFormat("test@gmail.com")).toBe(true);
    expect(isEmailFormat("wrong_test")).toBe(false);
})

test("test isMinLength",()=>{
    expect(isMinLength("123456",6)).toBe(true);
    expect(isMinLength("12345",6)).toBe(false);
})