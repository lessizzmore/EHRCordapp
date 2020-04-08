import { Component, OnInit, ViewChild } from '@angular/core';
import { EHR } from '../ehr';
import { MatTableDataSource } from '@angular/material/table';
import { MatPaginator } from '@angular/material/paginator';
import { EhrService } from '../services/ehr.service';

@Component({
  selector: 'app-target-ehr',
  templateUrl: './target-ehr.component.html',
  styleUrls: ['./target-ehr.component.scss']
})
export class TargetEhrComponent implements OnInit {

  displayedColumns: string[] = ['id', 'patient', 'origin', 'target', 'status', 'note', 'attachmentId'];
  ehrs: EHR[];
  dataSource: MatTableDataSource<EHR>;

  @ViewChild(MatPaginator, {static: true}) paginator: MatPaginator;


  constructor(private ehrSvc: EhrService) { }

  ngOnInit(): void {
    this.ehrSvc.getTargetEhrs().subscribe(
      ehrs => {
        this.ehrs = ehrs;
        console.log(this.ehrs)
        this.dataSource = new MatTableDataSource<EHR>(this.ehrs);
      }
    );
  }

}
