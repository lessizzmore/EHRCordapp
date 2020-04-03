import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';

@Injectable()
export class NotificationService {

    private navItemChangedSource = new BehaviorSubject<string>('');
    navItemObservable = this.navItemChangedSource.asObservable();

    private logChangedSource = new BehaviorSubject<boolean>(false);
    logoutObservable = this.logChangedSource.asObservable();

    emitSelectionChanged(selectedItem: string) {
        this.navItemChangedSource.next(selectedItem);
    }


    emitLogout(logout: boolean) {
        this.logChangedSource.next(logout);
    }
}
