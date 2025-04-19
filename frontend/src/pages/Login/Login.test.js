import { render, screen, fireEvent } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import Login from "./Login";  // 根据你的组件路径调整
import fetchMock from "jest-fetch-mock";
import {act} from "react";
import React from 'react';

// 启用 fetch 模拟
fetchMock.enableMocks();

describe("Login Component", () => {
    beforeEach(() => {
        fetchMock.resetMocks(); // 每次测试前重置 fetch
    });

    test("renders the login form", () => {
        render(
            <MemoryRouter>
                <Login />
            </MemoryRouter>
        );

        // 检查 UI 是否正确渲染
        expect(screen.getByLabelText(/username/i)).toBeInTheDocument();
        expect(screen.getByLabelText(/password/i)).toBeInTheDocument();
        expect(screen.getByRole("button", { name: /sign in/i })).toBeInTheDocument();
    });

    test("allows user to type in username and password fields", () => {
        render(
            <MemoryRouter>
                <Login />
            </MemoryRouter>
        );

        // 获取输入框
        const usernameInput = screen.getByLabelText(/username/i);
        const passwordInput = screen.getByLabelText(/password/i);

        // 模拟输入
        fireEvent.change(usernameInput, { target: { value: "testuser" } });
        fireEvent.change(passwordInput, { target: { value: "testpass" } });

        // 断言输入框是否更新
        expect(usernameInput.value).toBe("testuser");
        expect(passwordInput.value).toBe("testpass");
    });

    test("submits the form and calls the login API", async () => {
        fetchMock.mockResponseOnce(JSON.stringify({ success: true }), { status: 200 });

        render(
            <MemoryRouter>
                <Login />
            </MemoryRouter>
        );

        // 获取输入框和按钮
        const usernameInput = screen.getByLabelText(/username/i);
        const passwordInput = screen.getByLabelText(/password/i);
        const submitButton = screen.getByRole("button", { name: /sign in/i });

        // 模拟输入
        fireEvent.change(usernameInput, { target: { value: "testuser" } });
        fireEvent.change(passwordInput, { target: { value: "testpass" } });
// 使用 act 包装点击操作
        await act(async () => {
            fireEvent.click(submitButton);
        });

        // 确保 fetch 被调用
        expect(fetchMock).toHaveBeenCalledTimes(1);
        expect(fetchMock).toHaveBeenCalledWith(
            expect.stringContaining("/login"),
            expect.objectContaining({
                method: "POST",
                headers: { "Content-Type": "application/x-www-form-urlencoded",
                            "Accept": "application/json" },
                body: expect.stringContaining("username=testuser&password=testpass"),
            })
        );
    });

    test("handles login failure gracefully", async () => {
        fetchMock.mockResponseOnce(JSON.stringify({ error: "Invalid credentials" }), { status: 401 });

        render(
            <MemoryRouter>
                <Login />
            </MemoryRouter>
        );

        const usernameInput = screen.getByLabelText(/username/i);
        const passwordInput = screen.getByLabelText(/password/i);
        const submitButton = screen.getByRole("button", { name: /sign in/i });

        fireEvent.change(usernameInput, { target: { value: "wronguser" } });
        fireEvent.change(passwordInput, { target: { value: "wrongpass" } });

        // 使用 act 包装点击操作
        await act(async () => {
            fireEvent.click(submitButton);
        });

        // 可以检查 console.error 是否被调用
        expect(fetchMock).toHaveBeenCalledTimes(1);
    });
});
