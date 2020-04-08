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
    let id = this.paForm.get('id').value
    id = id.trim();
    this.ehrSvc.deletePatientEhr('O=PartyB,L=New York,C=US,CN=OriginDoctor', id).subscribe(
      data => {
        console.log(data)
      },
      error => {
        console.log('failed' + error)

      }
    );
    this.paForm.reset();
  }

  onSuspend() {

    let id = this.paForm.get('id').value
    id = id.trim();
    this.ehrSvc.suspendEhr('O=PartyB,L=New York,C=US,CN=OriginDoctor', id).subscribe(
      data => {
        console.log(data)
      },
      error => {
        console.log('failed' + error)

      }
    );
    this.paForm.reset();

  }

  onActivate() {

    let id = this.paForm.get('id').value
    id = id.trim();
    this.ehrSvc.activateEhr('O=PartyB,L=New York,C=US,CN=OriginDoctor', id).subscribe(
      data => {
        console.log(data)
      },
      error => {
        console.log('failed' + error)

      }
    );
    this.paForm.reset();

  }
}
