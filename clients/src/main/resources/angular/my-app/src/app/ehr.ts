import { Status } from './status';

export interface EHR {
    id: number
    patient: string,
    originDoctor: string,
    targetDoctor: string,
    status: Status,
    note: string,
    attachmentId: string
}