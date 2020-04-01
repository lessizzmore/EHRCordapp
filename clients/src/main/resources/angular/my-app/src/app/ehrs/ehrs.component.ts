import { Component, OnInit, ViewChild } from '@angular/core';
import { EHRS } from '../mock-ehrs';
import { EHR } from '../ehr';
import {MatPaginator} from '@angular/material/paginator';
import {MatTableDataSource} from '@angular/material/table';

@Component({
  selector: 'app-ehrs',
  templateUrl: './ehrs.component.html',
  styleUrls: ['./ehrs.component.scss']
})
export class EhrsComponent implements OnInit {
  displayedColumns: string[] = ['id', 'patient', 'origin', 'target', 'status', 'note', 'attachmentId', 'action'];
  ehrs = EHRS
  dataSource = new MatTableDataSource<EHR>(EHRS);

  @ViewChild(MatPaginator, {static: true}) paginator: MatPaginator;


  constructor() { }

  ngOnInit(): void {
    this.dataSource.paginator = this.paginator;
  }

}
