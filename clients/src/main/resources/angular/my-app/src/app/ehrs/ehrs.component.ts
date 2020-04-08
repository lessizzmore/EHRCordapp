import { Component, OnInit, ViewChild, Input } from '@angular/core';
import { EHR } from '../ehr';
import {MatPaginator} from '@angular/material/paginator';
import {MatTableDataSource} from '@angular/material/table';
import { EhrService } from '../services/ehr.service';

@Component({
  selector: 'app-ehrs',
  templateUrl: './ehrs.component.html',
  styleUrls: ['./ehrs.component.scss']
})
export class EhrsComponent implements OnInit {
  displayedColumns: string[] = ['patient', 'origin', 'target', 'status', 'note', 'attachmentId', 'action'];
  ehrs: EHR[];
  dataSource: MatTableDataSource<EHR>;

  @ViewChild(MatPaginator, {static: true}) paginator: MatPaginator;
  @Input() patient: string;
  @Input() targetD: string;
  @Input() originD: string;
  @Input() note: string;
  @Input() attachmentId: string;


  constructor(private ehrSvc: EhrService) { }

  ngOnInit(): void {
    this.ehrSvc.getOriginEhrs().subscribe(
      ehrs => {
        this.ehrs = ehrs;
        this.dataSource = new MatTableDataSource<EHR>(this.ehrs);
      }
    );
  }

  refresh() {
    
  }

  
}
