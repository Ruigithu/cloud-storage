import {render, screen, fireEvent} from "@testing-library/react";
import React from 'react';
import UploadProgress from "./UploadProgress";

describe("UploadProgress Component", () => {
    const mockOnPause = jest.fn();
    const mockOnResume = jest.fn();
    const mockOnCancel = jest.fn();

    const defaultProps = {
        progress: 50,
        isPaused: false,
        onPause: mockOnPause,
        onResume: mockOnResume,
        onCancel: mockOnCancel,
    };

    beforeEach(() => {
        jest.clearAllMocks();
    });

    test("testing whether UI elements got rendered", () => {
        render(<UploadProgress {...defaultProps} />);

        expect(screen.getByText(/Uploading: 50.00%/i)).toBeInTheDocument();
        expect(screen.getByText("Pause")).toBeInTheDocument();
        expect(screen.getByText("Cancel")).toBeInTheDocument();
    });

    test("testing whether progress percentage is displayed correctly", () => {
        render(<UploadProgress {...defaultProps} progress={75.5} />);

        expect(screen.getByText(/Uploading: 75.50%/i)).toBeInTheDocument();
    });

    test("testing whether progress bar width matches progress value", () => {
        const {container} = render(<UploadProgress {...defaultProps} progress={60} />);

        const progressBar = container.querySelector('div[style*="width: 60%"]');
        expect(progressBar).toBeInTheDocument();
    });

    test("testing whether additional info is displayed when provided", () => {
        render(<UploadProgress {...defaultProps} additionalInfo="(5 MB / 10 MB)" />);

        expect(screen.getByText(/Uploading: 50.00% \(5 MB \/ 10 MB\)/i)).toBeInTheDocument();
    });

    test("testing whether Pause button calls onPause when clicked", () => {
        render(<UploadProgress {...defaultProps} />);

        const pauseButton = screen.getByText("Pause");
        fireEvent.click(pauseButton);

        expect(mockOnPause).toHaveBeenCalledTimes(1);
    });

    test("testing whether Resume button is displayed when isPaused is true", () => {
        render(<UploadProgress {...defaultProps} isPaused={true} />);

        expect(screen.getByText("Resume")).toBeInTheDocument();
        expect(screen.queryByText("Pause")).not.toBeInTheDocument();
    });

    test("testing whether Resume button calls onResume when clicked", () => {
        render(<UploadProgress {...defaultProps} isPaused={true} />);

        const resumeButton = screen.getByText("Resume");
        fireEvent.click(resumeButton);

        expect(mockOnResume).toHaveBeenCalledTimes(1);
    });

    test("testing whether Cancel button calls onCancel when clicked", () => {
        render(<UploadProgress {...defaultProps} />);

        const cancelButton = screen.getByText("Cancel");
        fireEvent.click(cancelButton);

        expect(mockOnCancel).toHaveBeenCalledTimes(1);
    });

    test("testing whether Pause button is hidden when showPauseButton is false", () => {
        render(<UploadProgress {...defaultProps} showPauseButton={false} />);

        expect(screen.queryByText("Pause")).not.toBeInTheDocument();
        expect(screen.queryByText("Resume")).not.toBeInTheDocument();
        expect(screen.getByText("Cancel")).toBeInTheDocument();
    });

    test("testing whether progress displays 0% correctly", () => {
        render(<UploadProgress {...defaultProps} progress={0} />);

        expect(screen.getByText(/Uploading: 0.00%/i)).toBeInTheDocument();
    });

    test("testing whether progress displays 100% correctly", () => {
        render(<UploadProgress {...defaultProps} progress={100} />);

        expect(screen.getByText(/Uploading: 100.00%/i)).toBeInTheDocument();
    });

    test("testing whether component has fixed position styling", () => {
        const {container} = render(<UploadProgress {...defaultProps} />);

        const mainDiv = container.firstChild;
        expect(mainDiv).toHaveStyle({position: 'fixed'});
        expect(mainDiv).toHaveStyle({bottom: '20px'});
        expect(mainDiv).toHaveStyle({left: '20px'});
    });

    test("testing whether Pause button has correct background color when not paused", () => {
        render(<UploadProgress {...defaultProps} isPaused={false} />);

        const pauseButton = screen.getByText("Pause");
        expect(pauseButton).toHaveStyle({backgroundColor: '#ff9800'});
    });

    test("testing whether Resume button has correct background color when paused", () => {
        render(<UploadProgress {...defaultProps} isPaused={true} />);

        const resumeButton = screen.getByText("Resume");
        expect(resumeButton).toHaveStyle({backgroundColor: '#4caf50'});
    });

    test("testing whether Cancel button has correct background color", () => {
        render(<UploadProgress {...defaultProps} />);

        const cancelButton = screen.getByText("Cancel");
        expect(cancelButton).toHaveStyle({backgroundColor: '#f44336'});
    });
});