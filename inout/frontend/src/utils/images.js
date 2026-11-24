/** 사진이 없을 때 보여 줄 기본 상품 이미지 (frontend/public) */
export const DEFAULT_ITEM_IMAGE = '/default-product.svg';

/** 사진 파일 검사: 업로드 API와 같은 조건 (jpg·png·gif·webp, 10MB 이하) */
export function validateImageFile(file) {
  if (!file) return '사진을 선택해 주세요.';
  if (!/\.(jpe?g|png|gif|webp)$/i.test(file.name)) return 'jpg·png·gif·webp 사진만 올릴 수 있습니다.';
  if (file.size > 10 * 1024 * 1024) return '사진은 10MB 이하로 올려 주세요.';
  return null;
}
