import { Component, OnInit } from '@angular/core';


@Component({
  selector: 'app-messages-component',
  templateUrl: './messages-component.component.html',
  styleUrls: ['./messages-component.component.scss']
})
export class MessagesComponentComponent implements OnInit {

  constructor() { }
  msgs = [];


  ngOnInit() {
  }


  showMessage(message: string, error: boolean) {
    this.msgs = [];
    if (error) {
      this.msgs.push({ severity: 'error', summary: '', detail: message });

    } else {
      this.msgs.push({ severity: 'success', summary: '', detail: message });
    }
    setTimeout(() => {
      /** spinner ends after 5 seconds */
      this.msgs = [];
    }, 3000);
  }
 

}
