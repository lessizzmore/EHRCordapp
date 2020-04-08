import { Component, ViewChild, Output, Input, OnInit } from '@angular/core';
import { EhrsComponent } from './ehrs/ehrs.component';
import { Router } from '@angular/router';
import { PatientEhrComponent } from './patient-ehr/patient-ehr.component';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss']
})
export class AppComponent implements OnInit{
  title = 'my-app';
  navLinks: any[];
  activeLinkIndex = -1;



  @ViewChild(EhrsComponent) private ehrComponent: EhrsComponent;
  @ViewChild(PatientEhrComponent) private patientEhrComponent: PatientEhrComponent;

  constructor(private router: Router) { 
    this.navLinks = [
      {
          label: 'EHR Form',
          link: './form',
          index: 0
      }, {
          label: 'Origin Doctor',
          link: './origin',
          index: 1
      }, {
          label: 'Patient',
          link: './patient',
          index: 2
      }, {
        label: 'Target Doctor',
        link: './target',
        index: 3
      }
  ];
  }

  ngOnInit(): void {
    this.router.events.subscribe((res) => {
      this.activeLinkIndex = this.navLinks.indexOf(this.navLinks.find(tab => tab.link === '.' + this.router.url));
    });
  }

}

