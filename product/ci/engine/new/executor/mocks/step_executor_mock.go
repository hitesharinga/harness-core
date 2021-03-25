// Code generated by MockGen. DO NOT EDIT.
// Source: step_executor.go

// Package executor is a generated GoMock package.
package executor

import (
	context "context"
	gomock "github.com/golang/mock/gomock"
	proto "github.com/wings-software/portal/product/ci/engine/proto"
	reflect "reflect"
)

// MockStepExecutor is a mock of StepExecutor interface.
type MockStepExecutor struct {
	ctrl     *gomock.Controller
	recorder *MockStepExecutorMockRecorder
}

// MockStepExecutorMockRecorder is the mock recorder for MockStepExecutor.
type MockStepExecutorMockRecorder struct {
	mock *MockStepExecutor
}

// NewMockStepExecutor creates a new mock instance.
func NewMockStepExecutor(ctrl *gomock.Controller) *MockStepExecutor {
	mock := &MockStepExecutor{ctrl: ctrl}
	mock.recorder = &MockStepExecutorMockRecorder{mock}
	return mock
}

// EXPECT returns an object that allows the caller to indicate expected use.
func (m *MockStepExecutor) EXPECT() *MockStepExecutorMockRecorder {
	return m.recorder
}

// Run mocks base method.
func (m *MockStepExecutor) Run(ctx context.Context, step *proto.UnitStep) error {
	m.ctrl.T.Helper()
	ret := m.ctrl.Call(m, "Run", ctx, step)
	ret0, _ := ret[0].(error)
	return ret0
}

// Run indicates an expected call of Run.
func (mr *MockStepExecutorMockRecorder) Run(ctx, step interface{}) *gomock.Call {
	mr.mock.ctrl.T.Helper()
	return mr.mock.ctrl.RecordCallWithMethodType(mr.mock, "Run", reflect.TypeOf((*MockStepExecutor)(nil).Run), ctx, step)
}
