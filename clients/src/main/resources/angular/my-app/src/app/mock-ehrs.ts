import { EHR } from './ehr';
import { Status } from './status';

export const EHRS: EHR[] = [
    { id: 1, patient: "patient1", originDoctor: "origin d1", targetDoctor:"target d1", status: Status.pending, note: "note", attachmentId: "1" },
    { id: 2, patient: "patient1", originDoctor: "origin d1", targetDoctor:"target d1", status: Status.activated, note: "note", attachmentId: "1" },
    { id: 3, patient: "patient1", originDoctor: "origin d1", targetDoctor:"target d1", status: Status.shared, note: "note", attachmentId: "1" },
    { id: 4, patient: "patient1", originDoctor: "origin d1", targetDoctor:"target d1", status: Status.suspended, note: "note", attachmentId: "1" },
]