import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { PatientEhrComponent } from './patient-ehr.component';

describe('PatientEhrComponent', () => {
  let component: PatientEhrComponent;
  let fixture: ComponentFixture<PatientEhrComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ PatientEhrComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(PatientEhrComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
