import { render, screen, fireEvent } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import fetchMock from "jest-fetch-mock";
import {act} from "react";
import Signup from "./signup";
import React from 'react';
import Login from "../Login/login";

fetchMock.enableMocks();

describe("test signup function",()=>{
    beforeEach(
        ()=>{fetchMock.resetMocks();});

    test("test render signup form",()=>{
        render(
            <MemoryRouter>
                <Signup/>
            </MemoryRouter>
        );
        expect(screen.getByLabelText(/name/i)).toBeInTheDocument();
        expect(screen.getByLabelText(/email/i)).toBeInTheDocument();
        expect(screen.getByLabelText(/password/i)).toBeInTheDocument();

        expect(screen.getByRole("button",{name:/Sign up/i})).toBeInTheDocument();

    });

    test("allow user to type in name,email and password  field",()=>{
        render(
            <MemoryRouter>
                <Signup/>
            </MemoryRouter>
        );

        const nameInput = screen.getByLabelText(/name/i);
        const emailInput=screen.getByLabelText(/email/i);
        const passwordInput =screen.getByLabelText(/password/i);

        fireEvent.change(nameInput,{target:{value:"testname"}});
        fireEvent.change(emailInput,{target:{value:"testemail"}});
        fireEvent.change(passwordInput,{target:{value:"testpassword"}});

        expect(nameInput.value).toBe("testname");
        expect(emailInput.value).toBe("testemail");
        expect(passwordInput.value).toBe("testpassword");

    });

    test("submit the form and call the sign up API",async () => {
        fetchMock.mockResponseOnce(JSON.stringify({success: true}), {status: 200});
        render(
            <MemoryRouter>
                <Signup/>
            </MemoryRouter>
        );

        const nameInput = screen.getByLabelText(/name/i);
        const emailInput = screen.getByLabelText(/email/i);
        const passwordInput = screen.getByLabelText(/password/i);
        const submitButton = screen.getByRole("button", {name: /Sign up/i});

        fireEvent.change(nameInput, {target: {value: "testname"}});
        fireEvent.change(emailInput, {target: {value: "testemail"}});
        fireEvent.change(passwordInput, {target: {value: "testpassword"}});


        await act(async () => {
            fireEvent.click(submitButton)
        })

        expect(fetchMock).toBeCalledTimes(1);
        expect(fetchMock).toBeCalledWith(
            expect.stringContaining("/signup"),
            expect.objectContaining({
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                    "Accept": "application/json"
                },
                body: expect.stringContaining(JSON.stringify({
                    name: "testname",
                    email: "testemail",
                    passwordHash: "testpassword"
                }))
            }),
        )

    })

})