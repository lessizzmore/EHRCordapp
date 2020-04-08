import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse, HttpHeaders } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, of } from 'rxjs';
import { EHR } from '../ehr';
import { catchError, map, tap } from 'rxjs/operators';
import { MatOptionSelectionChange } from '@angular/material/core';



@Injectable()
export class EhrService {
    origin_doctor_resource: string;
    target_doctor_resource: string;
    patient_resource: string;

    constructor(private http: HttpClient, private router: Router) {
        this.origin_doctor_resource = 'http://localhost:10060/';
        this.patient_resource = 'http://localhost:10050/';
        this.target_doctor_resource = 'http://localhost:10070/';
    }




    getOriginEhrs():Observable<EHR[]> {
        const httpOptions = {
            headers: new HttpHeaders({
              'Content-Type':  'application/json'
            })
          };
        httpOptions.headers.append('Access-Control-Allow-Origin', '*');
        httpOptions.headers.append('Access-Control-Allow-Methods', 'GET,POST,OPTIONS,DELETE,PUT')
        httpOptions.headers.append('Access-Control-Allow-Headers', 'Content-Type, Authorization')
        const requestUrl = this.origin_doctor_resource + 'ehrs';
        return this.http.get<EHR[]>(requestUrl, httpOptions).pipe(
            catchError(this.handleError<EHR[]>('getEHRs', []))
          );
    }

    postEhr(ehr: EHR): Observable<EHR> {
        console.log("ehr patient:" + ehr.patient)
        const httpOptions = {
            headers: new HttpHeaders({
              'Content-Type':  'application/json'
            })
          };
        httpOptions.headers.append('Access-Control-Allow-Origin', '*');
        httpOptions.headers.append('Access-Control-Allow-Methods', 'GET,POST,OPTIONS,DELETE,PUT')
        httpOptions.headers.append('Access-Control-Allow-Headers', 'Content-Type, Authorization')
        const requestUrl = this.origin_doctor_resource
         + 'request?patient=' + ehr.patient
         + '&targetD=' + ehr.targetDoctor
         + '&note=' + ehr.note
         + '&attachmentId=' + ehr.attachmentId;
        console.log("url:" + requestUrl)
        return this.http.post<EHR>(requestUrl, ehr, httpOptions).pipe(
            catchError(this.handleError<EHR>('createEHR'))
          );
    }

    deleteEhr(counterParty: string, id:string): Observable<EHR> {
        const httpOptions = {
            headers: new HttpHeaders({
              'Content-Type':  'application/json'
            })
          };
        httpOptions.headers.append('Access-Control-Allow-Origin', '*');
        httpOptions.headers.append('Access-Control-Allow-Methods', 'GET,POST,OPTIONS,DELETE,PUT')
        httpOptions.headers.append('Access-Control-Allow-Headers', 'Content-Type, Authorization')
        const requestUrl = this.origin_doctor_resource
         + 'delete?counterParty=' + counterParty
         + '&ehrId=' + id;
        console.log("url:" + requestUrl)
        return this.http.post<EHR>(requestUrl, httpOptions).pipe(
            catchError(this.handleError<EHR>('deleteEHR'))
          );
    }


/**
 * Handle Http operation that failed.
 * Let the app continue.
 * @param operation - name of the operation that failed
 * @param result - optional value to return as the observable result
 */
private handleError<T> (operation = 'operation', result?: T) {
    return (error: any): Observable<T> => {
  
      // TODO: send the error to remote logging infrastructure
      console.error(error); // log to console instead
  
      // Let the app keep running by returning an empty result.
      return of(result as T);
    };
  }
}