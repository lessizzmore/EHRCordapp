import { Component, OnInit } from '@angular/core';
import { FormBuilder } from '@angular/forms';
import { EhrService } from '../services/ehr.service';
import { MessagesComponentComponent } from '../messages-component/messages-component.component';

@Component({
  selector: 'app-origin-action-form',
  templateUrl: './origin-action-form.component.html',
  styleUrls: ['./origin-action-form.component.scss']
})
export class OriginActionFormComponent extends MessagesComponentComponent implements OnInit {
  oaForm;
  errorMsg = "";


  constructor(private formBuilder: FormBuilder, private ehrSvc: EhrService) { 
    super();
    this.oaForm = this.formBuilder.group({
      id: '',
    });
  }

  ngOnInit(): void {
  }

  onDelete() {
    let id = this.oaForm.get('id').value
    id = id.trim();
    this.ehrSvc.deleteEhr('O=PartyA,L=New York,C=US,CN=Patient', id).subscribe(
      data => {
        console.log(data)
        this.errorMsg = "Ehr record with id: " + id + " deleted successfully."
        this.showMessage(this.errorMsg, false);
      },
      error => {
        console.log('failed' + error)
        this.errorMsg = "Delete failed."
        this.showMessage(this.errorMsg, true)
      }
    );
    this.oaForm.reset();
  }

  onShare() {

  }
}
