import { Component, OnInit, ViewChild, Input } from '@angular/core';
import { EHR } from '../ehr';
import {MatPaginator} from '@angular/material/paginator';
import {MatTableDataSource} from '@angular/material/table';
import { EhrService } from '../services/ehr.service';
import { OriginActionFormComponent } from '../origin-action-form/origin-action-form.component';

@Component({
  selector: 'app-ehrs',
  templateUrl: './ehrs.component.html',
  styleUrls: ['./ehrs.component.scss']
})
export class EhrsComponent implements OnInit {
  displayedColumns: string[] = ['id', 'patient', 'origin', 'target', 'status', 'note', 'attachmentId'];
  ehrs: EHR[];
  dataSource: MatTableDataSource<EHR>;

  @ViewChild(MatPaginator, {static: true}) paginator: MatPaginator;
  @ViewChild(OriginActionFormComponent) private originActionComponent: OriginActionFormComponent;


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
