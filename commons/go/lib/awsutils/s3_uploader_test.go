package awsutils

import (
	"context"
	"errors"
	"github.com/aws/aws-sdk-go/service/s3/s3manager"
	"github.com/golang/mock/gomock"
	"github.com/stretchr/testify/assert"
	"github.com/wings-software/portal/commons/go/lib/awsutils/mocks"
	"github.com/wings-software/portal/commons/go/lib/filesystem"
	"github.com/wings-software/portal/commons/go/lib/logs"
	"go.uber.org/zap"
	"io"

	"testing"
)

func TestS3Uploader_UploadReader(t *testing.T) {
	ctx, client, bucket, ul, key, reader := setupTestS3uploader(t)
	client.EXPECT().UploadWithContext(ctx, &s3manager.UploadInput{
		ACL:          &defaultACL,
		Bucket:       &bucket,
		Key:          &key,
		Body:         reader,
		StorageClass: &defaultStorageClass,
	}).Return(&s3manager.UploadOutput{}, nil)

	actBucket, actKey, err := ul.uploadReader(ctx, key, reader)
	assert.Equal(t, bucket, actBucket)
	assert.Equal(t, key, actKey)
	assert.NoError(t, err)
}

func TestS3Uploader_UploadReader_Err(t *testing.T) {
	ctx, client, bucket, ul, key, reader := setupTestS3uploader(t)
	client.EXPECT().UploadWithContext(ctx, &s3manager.UploadInput{
		ACL:          &defaultACL,
		Bucket:       &bucket,
		Key:          &key,
		Body:         reader,
		StorageClass: &defaultStorageClass,
	}).Return(&s3manager.UploadOutput{}, errors.New("some error"))

	actBucket, actKey, err := ul.uploadReader(ctx, key, reader)
	assert.Equal(t, bucket, actBucket)
	assert.Equal(t, key, actKey)
	assert.Error(t, err)
}

//setup_TestS3Uploader is helper function for TestS3Uploader* methods
func setupTestS3uploader(t *testing.T) (context.Context, *awsutils.MockS3UploadClient, string, *s3Uploader, string, *struct{ io.Reader }) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	client := awsutils.NewMockS3UploadClient(ctrl)
	fs := filesystem.NewMockFileSystem(ctrl)
	log, _ := logs.GetObservedLogger(zap.ErrorLevel)

	bucket := "bucket"
	ul := &s3Uploader{bucket, client, fs, log.Sugar()}

	key := "key"
	reader := &struct{ io.Reader }{}
	return ctx, client, bucket, ul, key, reader
}

func TestPrefixedS3Uploader_UploadReader(t *testing.T) {
	mockCtrl := gomock.NewController(t)
	defer mockCtrl.Finish()

	subUploader := awsutils.NewMockS3Uploader(mockCtrl)
	subUploader.EXPECT().UploadReader("prefix/file", gomock.Any()).Return("bucket", "x/y", nil)

	uploader := NewPrefixedS3Uploader(subUploader, "prefix/")
	bucket, key, err := uploader.UploadReader("file", nil)
	assert.Equal(t, "bucket", bucket)
	assert.Equal(t, "x/y", key)
	assert.NoError(t, err)
}
