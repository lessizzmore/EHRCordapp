import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { EhrFormComponent } from './ehr-form.component';

describe('EhrFormComponent', () => {
  let component: EhrFormComponent;
  let fixture: ComponentFixture<EhrFormComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ EhrFormComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(EhrFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
