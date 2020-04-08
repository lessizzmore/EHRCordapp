import { LinearId } from './linearid';

export class EHR {
    linearId: LinearId;
    patient: string;
    originDoctor: string;
    targetDoctor: string;
    status: string;
    note: string;
    attachmentId: string;
}