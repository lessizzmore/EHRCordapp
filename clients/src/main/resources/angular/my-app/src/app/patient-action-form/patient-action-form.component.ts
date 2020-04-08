import { Component, OnInit } from '@angular/core';
import { FormBuilder } from '@angular/forms';
import { EhrService } from '../services/ehr.service';

@Component({
  selector: 'app-patient-action-form',
  templateUrl: './patient-action-form.component.html',
  styleUrls: ['./patient-action-form.component.scss']
})
export class PatientActionFormComponent implements OnInit {

  paForm;

  constructor(private formBuilder: FormBuilder, private ehrSvc: EhrService) { 
    this.paForm = this.formBuilder.group({
      id: '',
    });
  }

  ngOnInit(): void {
  }


  onDelete() {

  }

  onSuspend() {

  }

  onActivate() {

  }
}
