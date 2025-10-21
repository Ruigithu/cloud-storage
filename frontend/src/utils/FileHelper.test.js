import { formatFileSize, getFileIcon, FILE_ICON_COLORS } from './fileHelper';

describe('fileHelper', () => {
    describe('formatFileSize', () => {
        test('should return "0 Bytes" for zero bytes', () => {
            const result = formatFileSize(0);

            expect(result).toBe('0 Bytes');
        });

        test('should format bytes correctly', () => {
            const result = formatFileSize(500);

            expect(result).toBe('500 Bytes');
        });

        test('should format kilobytes correctly', () => {
            const result = formatFileSize(1024);

            expect(result).toBe('1 KB');
        });

        test('should format kilobytes with decimals', () => {
            const result = formatFileSize(1536);

            expect(result).toBe('1.5 KB');
        });

        test('should format megabytes correctly', () => {
            const result = formatFileSize(1048576);

            expect(result).toBe('1 MB');
        });

        test('should format megabytes with decimals', () => {
            const result = formatFileSize(5242880);

            expect(result).toBe('5 MB');
        });

        test('should format gigabytes correctly', () => {
            const result = formatFileSize(1073741824);

            expect(result).toBe('1 GB');
        });

        test('should format large file sizes', () => {
            const result = formatFileSize(10737418240);

            expect(result).toBe('10 GB');
        });

        test('should round to 2 decimal places', () => {
            const result = formatFileSize(1234567);

            expect(result).toBe('1.18 MB');
        });
    });

    describe('getFileIcon', () => {
        test('should return image icon for image mime types', () => {
            const result = getFileIcon('image/png');

            expect(result).toBe('fa-regular fa-image');
        });

        test('should return image icon for image/jpeg', () => {
            const result = getFileIcon('image/jpeg');

            expect(result).toBe('fa-regular fa-image');
        });

        test('should return video icon for video mime types', () => {
            const result = getFileIcon('video/mp4');

            expect(result).toBe('fa-regular fa-file-video');
        });

        test('should return audio icon for audio mime types', () => {
            const result = getFileIcon('audio/mp3');

            expect(result).toBe('fa-regular fa-file-audio');
        });

        test('should return PDF icon for PDF mime type', () => {
            const result = getFileIcon('application/pdf');

            expect(result).toBe('fa-regular fa-file-pdf');
        });

        test('should return Word icon for Word documents', () => {
            const result = getFileIcon('application/msword');

            expect(result).toBe('fa-regular fa-file-word');
        });

        test('should return Word icon for .docx documents', () => {
            const result = getFileIcon('application/vnd.openxmlformats-officedocument.wordprocessingml.document');

            expect(result).toBe('fa-regular fa-file-word');
        });

        test('should return Excel icon for Excel files', () => {
            const result = getFileIcon('application/vnd.ms-excel');

            expect(result).toBe('fa-regular fa-file-excel');
        });

        test('should return PowerPoint icon for PowerPoint files', () => {
            const result = getFileIcon('application/vnd.ms-powerpoint');

            expect(result).toBe('fa-regular fa-file-powerpoint');
        });

        test('should return generic file icon for unknown types', () => {
            const result = getFileIcon('application/octet-stream');

            expect(result).toBe('fa-regular fa-file');
        });

        test('should return generic file icon for text files', () => {
            const result = getFileIcon('text/plain');

            expect(result).toBe('fa-regular fa-file');
        });
    });

    describe('FILE_ICON_COLORS', () => {
        test('should have color for image icon', () => {
            expect(FILE_ICON_COLORS['fa-regular fa-image']).toBe('#007cdb');
        });

        test('should have color for video icon', () => {
            expect(FILE_ICON_COLORS['fa-regular fa-file-video']).toBe('#ff4500');
        });

        test('should have color for audio icon', () => {
            expect(FILE_ICON_COLORS['fa-regular fa-file-audio']).toBe('#32cd32');
        });

        test('should have color for PDF icon', () => {
            expect(FILE_ICON_COLORS['fa-regular fa-file-pdf']).toBe('#ff0000');
        });

        test('should have color for Word icon', () => {
            expect(FILE_ICON_COLORS['fa-regular fa-file-word']).toBe('#2b579a');
        });

        test('should have color for Excel icon', () => {
            expect(FILE_ICON_COLORS['fa-regular fa-file-excel']).toBe('#217346');
        });

        test('should have color for PowerPoint icon', () => {
            expect(FILE_ICON_COLORS['fa-regular fa-file-powerpoint']).toBe('#d24726');
        });

        test('should have color for generic file icon', () => {
            expect(FILE_ICON_COLORS['fa-regular fa-file']).toBe('#808080');
        });

        test('should have exactly 8 color mappings', () => {
            const colorCount = Object.keys(FILE_ICON_COLORS).length;

            expect(colorCount).toBe(8);
        });
    });
});