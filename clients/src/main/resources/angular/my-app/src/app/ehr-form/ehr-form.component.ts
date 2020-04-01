import { Component, OnInit } from '@angular/core';

@Component({
  selector: 'app-ehr-form',
  templateUrl: './ehr-form.component.html',
  styleUrls: ['./ehr-form.component.scss']
})
export class EhrFormComponent implements OnInit {

  patients = ['patient 1', 'patient 2', 'patient 3'];
  targetDoctors = ['targetDoctor 1', 'targetDoctor 2'];


  constructor() { }

  ngOnInit(): void {
  }

}
