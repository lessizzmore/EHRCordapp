import { Component, OnInit } from '@angular/core';
import { FormBuilder } from '@angular/forms';
import { EhrService } from '../services/ehr.service';

@Component({
  selector: 'app-origin-action-form',
  templateUrl: './origin-action-form.component.html',
  styleUrls: ['./origin-action-form.component.scss']
})
export class OriginActionFormComponent implements OnInit {
  oaForm;

  constructor(private formBuilder: FormBuilder, private ehrSvc: EhrService) { 
    this.oaForm = this.formBuilder.group({
      id: '',
    });
  }

  ngOnInit(): void {
  }

  onDelete() {

  }

  onShare() {

  }
}
