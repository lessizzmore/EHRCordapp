import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { EhrsComponent } from './ehrs.component';

describe('EhrsComponent', () => {
  let component: EhrsComponent;
  let fixture: ComponentFixture<EhrsComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ EhrsComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(EhrsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
