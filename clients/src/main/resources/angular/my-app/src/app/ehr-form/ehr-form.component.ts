import { Component, OnInit } from '@angular/core';
import { EhrService } from '../services/ehr.service';
import { FormBuilder } from '@angular/forms';
import { EHR } from '../ehr';


@Component({
  selector: 'app-ehr-form',
  templateUrl: './ehr-form.component.html',
  styleUrls: ['./ehr-form.component.scss']
})
export class EhrFormComponent implements OnInit {
  ehrForm;
  ehr: EHR;
  constructor(private ehrSvc: EhrService, private formBuilder: FormBuilder) {
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

}
