import {render, screen, fireEvent, waitFor} from "@testing-library/react";
import {MemoryRouter} from "react-router-dom";
import React from 'react';
import Login from "./login";

const mockNavigate = jest.fn();

jest.mock("react-router-dom",()=>({
    ...jest.requireActual("react-router-dom"),
    useNavigate:()=>mockNavigate,
}));
global.fetch = jest.fn();

describe("Login Component",()=>{
    beforeEach(()=>{
        render(
            <MemoryRouter>
                <Login/>
            </MemoryRouter>
        );
    });


    test("testing whether UI elements got rendered",()=>{
        expect(screen.getByRole("button",{name:/sign in/i})).toBeInTheDocument();
        expect(screen.getByPlaceholderText("yourname@example.com",{exact:true})).toBeInTheDocument();
        expect(screen.getByPlaceholderText("••••••••",{exact:true})).toBeInTheDocument();
    });

    test("testing whether the initial states are correct",()=>{
        expect(screen.getByPlaceholderText("yourname@example.com",{exact:true})).toHaveValue("");
        expect(screen.getByPlaceholderText("••••••••",{exact:true})).toHaveValue("");
    });

    test("testing whether changing the input of email&password can lead to real-time change",()=>{

        const {emailInput,passwordInput} = setTheValidTestUsernameAndPassword();
        expect(emailInput).toHaveValue("test@gmail.com");
        expect(passwordInput).toHaveValue("123456");
    });

    test("testing whether clicking the sign in button will lead to button-disabled",()=>{
        setTheValidTestUsernameAndPassword();
        const { signInButton } =getTheSignInButton();
        fireEvent.click(signInButton);
        expect(signInButton).toBeDisabled();
    });

    test("testing whether the errors can be rendered when input the invalid emails",async ()=>{
        const {signInButton} = getTheSignInButton();

        fireEvent.click(signInButton);

        await waitFor(()=> {
                expect(screen.getByText(/email can't be empty/i, {exact: true})).toBeInTheDocument();
                expect(screen.getByText(/password can't be empty/i, {exact: true})).toBeInTheDocument();
            }
        )
    });

    test("testing whether the errors can get disappeared when change the input to the valid content",async ()=>{

        const{emailInput,passwordInput}=setTheValidTestUsernameAndPassword();

        fireEvent.blur(emailInput);
        fireEvent.blur(passwordInput);

        await waitFor(()=> {
                expect(screen.queryByText(/email can't be empty/i, {exact: true})).not.toBeInTheDocument();
                expect(screen.queryByText(/password can't be empty/i, {exact: true})).not.toBeInTheDocument();
            }
        )
    });

    test("testing when login fails without error message and displays default error", async () => {
        fetch.mockRejectedValueOnce(new Error());

        setTheValidTestUsernameAndPassword();
        const {signInButton} = getTheSignInButton();
        fireEvent.click(signInButton);

        await waitFor(() => {
            expect(screen.getByText(/login failed/i,{exact:true})).toBeInTheDocument();
        });
    });

    test("testing when handleSubmit successfully and navigate to home",async ()=>{

        prepareTheSuccessResponse();
        setTheValidTestUsernameAndPassword();
        const {signInButton}=getTheSignInButton();
        fireEvent.click(signInButton);

        await waitFor(()=>{

            expect(mockNavigate).toBeCalledWith("/home");
        });

    });

    test("testing when fail to handleSubmit and display errors",async ()=>{

        prepareTheFailureResponse();
        setTheInvalidTestUsernameAndPassword();
        const {signInButton}=getTheSignInButton();
        fireEvent.click(signInButton);

        await waitFor(()=>{
            expect(screen.getByText("invalid email or password",{exact:true})).toBeInTheDocument();
        })

    });

});
function setTheValidTestUsernameAndPassword(){
    const {emailInput,passwordInput} = getTheTwoInputFrameWork();
    fireEvent.change(emailInput,{target:{value:"test@gmail.com"}});
    fireEvent.change(passwordInput,{target:{value:"123456"}});
    return{emailInput,passwordInput};
}
function setTheInvalidTestUsernameAndPassword(){
    const {emailInput,passwordInput} = getTheTwoInputFrameWork();
    fireEvent.change(emailInput,{target:{value:"testWrong@gmail.com"}});
    fireEvent.change(passwordInput,{target:{value:"000000"}});
    return{emailInput,passwordInput};
}


function getTheTwoInputFrameWork(){
    const emailInput = screen.getByPlaceholderText("yourname@example.com",{exact:true});
    const passwordInput = screen.getByPlaceholderText("••••••••",{exact:true});
    return {emailInput,passwordInput};
}

function getTheSignInButton(){
    const signInButton = screen.getByRole("button",{name:/sign in/i});
    return {signInButton};
}

function prepareTheSuccessResponse(){
    fetch.mockResolvedValueOnce({
        ok: true,
        json: async () => ({ token: "123456",username:"jadeisme667@gmail.com" }),
    });
}

function prepareTheFailureResponse(){
    fetch.mockResolvedValueOnce({
        ok: false,
        json: async () => ({message:"invalid email or password"}),
    });
}
