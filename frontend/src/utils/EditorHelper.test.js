import {
    createQuillConfig,
    getFileExtension,
    isWordDocument,
    isLegacyWordDocument,
    isImageFile,
    isJsonFile,
    formatLastSaved,
    createFile,
    TOOLBAR_OPTIONS
} from './editorHelper';

describe('editorHelper', () => {
    describe('createQuillConfig', () => {
        test('should return valid Quill configuration object', () => {
            const config = createQuillConfig();

            expect(config).toHaveProperty('modules');
            expect(config).toHaveProperty('theme');
            expect(config).toHaveProperty('placeholder');
        });

        test('should include toolbar configuration', () => {
            const config = createQuillConfig();

            expect(config.modules.toolbar).toBe(TOOLBAR_OPTIONS);
        });

        test('should include history configuration', () => {
            const config = createQuillConfig();

            expect(config.modules.history).toEqual({
                delay: 2000,
                maxStack: 500,
                userOnly: true
            });
        });

        test('should use snow theme', () => {
            const config = createQuillConfig();

            expect(config.theme).toBe('snow');
        });

        test('should have placeholder text', () => {
            const config = createQuillConfig();

            expect(config.placeholder).toBe('Start editing the file...');
        });
    });

    describe('getFileExtension', () => {
        test('should return .json for application/json', () => {
            const ext = getFileExtension('application/json');

            expect(ext).toBe('.json');
        });

        test('should return .docx for Word document mime type', () => {
            const ext = getFileExtension('application/vnd.openxmlformats-officedocument.wordprocessingml.document');

            expect(ext).toBe('.docx');
        });

        test('should return .doc for legacy Word document', () => {
            const ext = getFileExtension('application/msword');

            expect(ext).toBe('.doc');
        });

        test('should return .png for image/png', () => {
            const ext = getFileExtension('image/png');

            expect(ext).toBe('.png');
        });

        test('should return .jpg for image/jpeg', () => {
            const ext = getFileExtension('image/jpeg');

            expect(ext).toBe('.jpg');
        });

        test('should return .pdf for application/pdf', () => {
            const ext = getFileExtension('application/pdf');

            expect(ext).toBe('.pdf');
        });

        test('should return .txt for unknown mime type', () => {
            const ext = getFileExtension('unknown/type');

            expect(ext).toBe('.txt');
        });

        test('should return .txt for empty mime type', () => {
            const ext = getFileExtension('');

            expect(ext).toBe('.txt');
        });
    });

    describe('isWordDocument', () => {
        test('should return true for .docx mime type', () => {
            const result = isWordDocument('application/vnd.openxmlformats-officedocument.wordprocessingml.document');

            expect(result).toBe(true);
        });

        test('should return false for .doc mime type', () => {
            const result = isWordDocument('application/msword');

            expect(result).toBe(false);
        });

        test('should return false for image mime type', () => {
            const result = isWordDocument('image/png');

            expect(result).toBe(false);
        });

        test('should return false for null mime type', () => {
            const result = isWordDocument(null);

            expect(result).toBe(false);
        });

        test('should return false for undefined mime type', () => {
            const result = isWordDocument(undefined);

            expect(result).toBe(false);
        });

        test('should return false for empty string', () => {
            const result = isWordDocument('');

            expect(result).toBe(false);
        });
    });

    describe('isLegacyWordDocument', () => {
        test('should return true for .doc mime type', () => {
            const result = isLegacyWordDocument('application/msword');

            expect(result).toBe(true);
        });

        test('should return false for .docx mime type', () => {
            const result = isLegacyWordDocument('application/vnd.openxmlformats-officedocument.wordprocessingml.document');

            expect(result).toBe(false);
        });

        test('should return false for image mime type', () => {
            const result = isLegacyWordDocument('image/png');

            expect(result).toBe(false);
        });

        test('should return false for null mime type', () => {
            const result = isLegacyWordDocument(null);

            expect(result).toBe(false);
        });

        test('should return false for undefined mime type', () => {
            const result = isLegacyWordDocument(undefined);

            expect(result).toBe(false);
        });

        test('should return false for empty string', () => {
            const result = isLegacyWordDocument('');

            expect(result).toBe(false);
        });
    });

    describe('isImageFile', () => {
        test('should return true for image/png', () => {
            const result = isImageFile('image/png');

            expect(result).toBe(true);
        });

        test('should return true for image/jpeg', () => {
            const result = isImageFile('image/jpeg');

            expect(result).toBe(true);
        });

        test('should return true for image/gif', () => {
            const result = isImageFile('image/gif');

            expect(result).toBe(true);
        });

        test('should return false for application/pdf', () => {
            const result = isImageFile('application/pdf');

            expect(result).toBe(false);
        });

        test('should return false for null mime type', () => {
            const result = isImageFile(null);

            expect(result).toBe(false);
        });

        test('should return false for undefined mime type', () => {
            const result = isImageFile(undefined);

            expect(result).toBe(false);
        });
    });

    describe('isJsonFile', () => {
        test('should return true for application/json', () => {
            const result = isJsonFile('application/json');

            expect(result).toBe(true);
        });

        test('should return false for application/pdf', () => {
            const result = isJsonFile('application/pdf');

            expect(result).toBe(false);
        });

        test('should return false for image/png', () => {
            const result = isJsonFile('image/png');

            expect(result).toBe(false);
        });

        test('should return false for null mime type', () => {
            const result = isJsonFile(null);

            expect(result).toBe(false);
        });

        test('should return false for undefined mime type', () => {
            const result = isJsonFile(undefined);

            expect(result).toBe(false);
        });
    });

    describe('formatLastSaved', () => {
        test('should return "Not saved yet" for null', () => {
            const result = formatLastSaved(null);

            expect(result).toBe('Not saved yet');
        });

        test('should format valid date with time', () => {
            const testDate = new Date('2024-01-15T14:30:45');
            const result = formatLastSaved(testDate);

            expect(result).toContain('Last saved:');
            expect(result).toContain(testDate.toLocaleTimeString());
        });

        test('should handle different date objects', () => {
            const testDate = new Date('2024-12-25T23:59:59');
            const result = formatLastSaved(testDate);

            expect(result).toBe(`Last saved: ${testDate.toLocaleTimeString()}`);
        });
    });

    describe('createFile', () => {
        test('should create File object with correct properties', () => {
            const blob = new Blob(['test content'], { type: 'text/plain' });
            const fileName = 'test.txt';
            const mimeType = 'text/plain';

            const file = createFile(blob, fileName, mimeType);

            expect(file).toBeInstanceOf(File);
            expect(file.name).toBe(fileName);
            expect(file.type).toBe(mimeType);
        });

        test('should create File with docx mime type', () => {
            const blob = new Blob([''], { type: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document' });
            const fileName = 'document.docx';
            const mimeType = 'application/vnd.openxmlformats-officedocument.wordprocessingml.document';

            const file = createFile(blob, fileName, mimeType);

            expect(file.name).toBe('document.docx');
            expect(file.type).toBe(mimeType);
        });

        test('should create File with image mime type', () => {
            const blob = new Blob([''], { type: 'image/png' });
            const fileName = 'image.png';
            const mimeType = 'image/png';

            const file = createFile(blob, fileName, mimeType);

            expect(file.name).toBe('image.png');
            expect(file.type).toBe('image/png');
        });
    });
});