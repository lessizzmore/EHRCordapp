import { Component, OnInit, ViewChild, ElementRef } from '@angular/core';
import { EhrService } from '../services/ehr.service';
import { FormBuilder } from '@angular/forms';
import { EHR } from '../ehr';
import { UploadService } from '../services/upload.service';
import { map, catchError } from 'rxjs/operators';
import { HttpEventType, HttpErrorResponse } from '@angular/common/http';
import { of } from 'rxjs';


@Component({
  selector: 'app-ehr-form',
  templateUrl: './ehr-form.component.html',
  styleUrls: ['./ehr-form.component.scss']
})
export class EhrFormComponent implements OnInit {
  ehrForm;
  ehr: EHR;
  @ViewChild("fileUpload", {static: false}) fileUpload: ElementRef;
  files  = [];  


  constructor(private ehrSvc: EhrService, private formBuilder: FormBuilder,private uploadService: UploadService) {
    this.ehrForm = this.formBuilder.group({
      patient: '',
      targetDoctor: '',
      note: '',
      attachmentId: ''
    });
   }

  ngOnInit(): void {
  }

  onSubmit(data) {
    this.ehr = new EHR();
    this.ehr.patient = data.patient;
    this.ehr.targetDoctor = data.targetDoctor;
    this.ehr.note = data.note;
    this.ehr.attachmentId = data.attachmentId;    
    console.log("data.patient:" + data.patient)
    this.ehrSvc.postEhr(this.ehr).subscribe(
      ehr => {
        console.log(ehr);
      }
    );
    console.warn('Your form has been submitted', data);
  }

  uploadFile(file) {  
    const formData = new FormData();  
    formData.append('file', file.data);  
    file.inProgress = true;  
    this.uploadService.upload(formData).pipe(  
      map(event => {  
        switch (event.type) {  
          case HttpEventType.UploadProgress:  
            file.progress = Math.round(event.loaded * 100 / event.total);  
            break;  
          case HttpEventType.Response:  
            return event;  
        }  
      }),  
      catchError((error: HttpErrorResponse) => {  
        file.inProgress = false;  
        return of(`${file.data.name} upload failed.`);  
      })).subscribe((event: any) => {  
        if (typeof (event) === 'object') {  
          console.log(event.body);  
        }  
      });  
  }

  uploadFiles() {  
    this.fileUpload.nativeElement.value = '';  
    this.files.forEach(file => {  
      this.uploadFile(file);  
    });  
  }

  onClick() {  
    const fileUpload = this.fileUpload.nativeElement;fileUpload.onchange = () => {  
    for (let index = 0; index < fileUpload.files.length; index++)  
    {  
     const file = fileUpload.files[index];  
     this.files.push({ data: file, inProgress: false, progress: 0});  
    }  
      this.uploadFiles();  
    };  
    fileUpload.click();  
}

}
