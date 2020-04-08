import { Component, OnInit, ViewChild } from '@angular/core';
import { MatTableDataSource } from '@angular/material/table';
import { EHR } from '../ehr';
import { MatPaginator } from '@angular/material/paginator';
import { OriginActionFormComponent } from '../origin-action-form/origin-action-form.component';
import { EhrService } from '../services/ehr.service';
import { PatientActionFormComponent } from '../patient-action-form/patient-action-form.component';

@Component({
  selector: 'app-patient-ehr',
  templateUrl: './patient-ehr.component.html',
  styleUrls: ['./patient-ehr.component.scss']
})
export class PatientEhrComponent implements OnInit {

  displayedColumns: string[] = ['id', 'patient', 'origin', 'target', 'status', 'note', 'attachmentId'];
  ehrs: EHR[];
  dataSource: MatTableDataSource<EHR>;

  @ViewChild(MatPaginator, {static: true}) paginator: MatPaginator;
  @ViewChild(PatientActionFormComponent) private patientActionComponent: PatientActionFormComponent;

  constructor(private ehrSvc: EhrService) { }

  ngOnInit(): void {
    this.ehrSvc.getOriginEhrs().subscribe(
      ehrs => {
        this.ehrs = ehrs;
        console.log(this.ehrs)
        this.dataSource = new MatTableDataSource<EHR>(this.ehrs);
      }
    );
  }

}
