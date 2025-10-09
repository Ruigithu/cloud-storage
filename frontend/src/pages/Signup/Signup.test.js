import {render, screen, fireEvent, waitFor} from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import Signup from "./signup";
import React from 'react';
import {MIN_PASSWORD_LENGTH} from "../../utils/validators";

const mockNavigate = jest.fn();

jest.mock("react-router-dom",()=>({
    ...jest.requireActual("react-router-dom"),
    useNavigate:()=>mockNavigate,
}));
global.fetch = jest.fn();


describe("SignUp Component",()=> {
    beforeEach (() =>
        render(
            <MemoryRouter>
                <Signup/>
            </MemoryRouter>
        )
    );

    test("test the UI elements in the web page", () => {
        const {emailInput, passwordInput, fullNameInput} = getTheThreeInputFrameWork();
        const{signUpButton} = getTheSignUpButton();
        expect(signUpButton).toBeInTheDocument();
        expect(emailInput).toBeInTheDocument();
        expect(passwordInput).toBeInTheDocument();
        expect(fullNameInput).toBeInTheDocument();
    });
    test("test the initial values are correct", () => {
        const {emailInput, passwordInput, fullNameInput} = getTheThreeInputFrameWork();
        expect(emailInput).toHaveValue("");
        expect(passwordInput).toHaveValue("");
        expect(fullNameInput).toHaveValue("");
    });

    test("testing whether changing the input of email&password&username can lead to real-time change",()=>{

        const {emailInput,passwordInput,fullNameInput} = setTheValidInput();
        expect(emailInput).toHaveValue("test@gmail.com");
        expect(passwordInput).toHaveValue("123456");
        expect(fullNameInput).toHaveValue("Alex Green");
    });

    test("testing whether clicking the sign in button will lead to button-disabled",()=>{
        setTheValidInput();
        const { signUpButton } =getTheSignUpButton();
        fireEvent.click(signUpButton);
        expect(signUpButton).toBeDisabled();
    });

    test("testing whether the errors can be rendered when input nothing",async ()=>{
        const signUpButton = getTheSignUpButton();

        fireEvent.click(signUpButton);

        await waitFor(()=> {
                expect(screen.getByText(/email can't be empty/i, {exact: true})).toBeInTheDocument();
                expect(screen.getByText(/password can't be empty/i, {exact: true})).toBeInTheDocument();
                expect(screen.getByText(/Name can't be empty/i, {exact: true})).toBeInTheDocument();
            }
        )
    });

    test("testing whether the errors can be rendered when input invalid data",async ()=>{
        setTheInvalidInput();

        fireEvent.click(getTheSignUpButton());

        await waitFor(()=> {
                expect(screen.getByText(/invalid email address/i, {exact: true})).toBeInTheDocument();
                expect(screen.getByText(/password at least has 6 characters/i, {exact: true})).toBeInTheDocument();
                expect(screen.getByText(/Name can't be empty/i, {exact: true})).toBeInTheDocument();
            }
        )
    });

    test("testing whether the errors get disappeared when input valid data",async ()=>{
        const{emailInput,passwordInput,fullNameInput}=setTheValidInput();

        fireEvent.blur(emailInput);
        fireEvent.blur(passwordInput);
        fireEvent.blur(fullNameInput);

        await waitFor(()=> {
                expect(screen.queryByText(/invalid email address/i, {exact: true})).not.toBeInTheDocument();
                expect(screen.queryByText(/password at least has `6` characters/i, {exact: true})).not.toBeInTheDocument();
                expect(screen.queryByText(/Name can't be empty/i, {exact: true})).not.toBeInTheDocument();
            }
        );
    });

    test("testing when handleSubmit successfully and navigate to login",async ()=>{

        prepareTheSuccessResponse();
        setTheValidInput();
        fireEvent.click(getTheSignUpButton());

        await waitFor(()=>{
            expect(mockNavigate).toBeCalledWith("/login");
        });
    });

    test("testing when fail to handleSubmit and display errors",async ()=>{

        prepareTheFailureResponse();
        setTheValidInput();
        fireEvent.click(getTheSignUpButton());

        await waitFor(()=>{
            expect(screen.getByText("Signup failed",{exact:true})).toBeInTheDocument();
        })
    });

    test("testing when fail to handleSubmit and display errors with the empty error message in the response",async ()=>{

        prepareTheFailureResponseWithEmptyErrorMessage();
        setTheValidInput();
        fireEvent.click(getTheSignUpButton());

        await waitFor(()=>{
            expect(screen.getByText("Registration failed. Please try again.",{exact:true})).toBeInTheDocument();
        })
    });

    test("testing when handleSubmit successfully and navigate to home",async ()=>{

        prepareTheSuccessResponse();
        setTheValidInput();
        fireEvent.click(getTheSignUpButton());

        await waitFor(()=>{

            expect(mockNavigate).toBeCalledWith("/login");
        });

    });

});


function getTheThreeInputFrameWork() {
    const emailInput= screen.getByPlaceholderText(/yourname@example.com/i,{exact:true});
    const passwordInput=screen.getByPlaceholderText(/••••••••/i,{exact:true});
    const fullNameInput=screen.getByPlaceholderText(/Rui Peng/i,{exact:true});

    return {emailInput,passwordInput,fullNameInput};

}
function getTheSignUpButton() {
    return screen.getByRole("button", {name: /Sign Up/i});
}
function setTheValidInput() {

    const {emailInput, passwordInput,fullNameInput} = getTheThreeInputFrameWork();
    fireEvent.change(emailInput, {target: {value: "test@gmail.com"}});
    fireEvent.change(passwordInput, {target: {value: "123456"}});
    fireEvent.change(fullNameInput, {target:{value:"Alex Green"}});
    return {emailInput, passwordInput,fullNameInput};
}

function setTheInvalidInput() {
    const {emailInput, passwordInput,fullNameInput} = getTheThreeInputFrameWork();
    fireEvent.change(emailInput, {target: {value: "WrongTestEmail"}});
    fireEvent.change(passwordInput, {target: {value: "000"}});
    fireEvent.change(fullNameInput, {target:{value:""}});
    return {emailInput, passwordInput,fullNameInput};
}

function prepareTheSuccessResponse(){
    fetch.mockResolvedValueOnce({
        ok: true,
        json: async () => ({message:"User registered successfully" }),
    });
}

function prepareTheFailureResponse(){
    fetch.mockResolvedValueOnce({
        ok: false,
        json: async () => ({message:"Signup failed"}),
    });
}

function prepareTheFailureResponseWithEmptyErrorMessage(){
    fetch.mockResolvedValueOnce({
        ok: false,
        json: async () => ({}),
    });
}