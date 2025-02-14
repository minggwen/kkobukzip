import { useEffect, useState } from "react";
import { Helmet } from "react-helmet-async";
import { useLocation, useNavigate, useParams } from "react-router-dom";

import {
  AdminAssignDocumentDataType,
  AdminBreedDocumentDataType,
  AdminDeathDocumentDataType,
} from "../../../types/document";

import AdminBreedDocsCheck from "../../../components/user/admin/AdminBreedDocsCheck";
import AdminAssignGrantDocsCheck from "../../../components/user/admin/AdminAssignGrantDocsCheck";
import AdminDeathDocsCheck from "../../../components/user/admin/AdminDeathDocsCheck";
import {
  approveDocumentRequest,
  getDetailDocumentData,
} from "../../../apis/documentApis";
import Header from "../../../components/common/Header";

type AdminDocType = "인공증식증명서" | "양도양수확인서" | "폐사질병신고서";
type dataType =
  | AdminBreedDocumentDataType
  | AdminAssignDocumentDataType
  | AdminDeathDocumentDataType;

function AdminDocsDetailPage() {
  const params = useParams();
  const location = useLocation();
  const navigate = useNavigate();
  const [layout, setLayout] = useState<AdminDocType | null>(null);

  const [data, setData] = useState<dataType | null>(null);

  useEffect(() => {
    const documentType: AdminDocType = location.state?.documentType ?? null;

    const getData = async () => {
      if (!params?.turtleUUID || !params?.documentHash) {
        return false;
      }
      // 여기서 대충 data fetch해서, data의 docType에 따라 swtich and data set
      const { success, data, message, error } = await getDetailDocumentData(
        params?.turtleUUID,
        params?.documentHash
      );
      if (!success || !data) {
        console.error(message, error);
        return false;
      }
      setData(data);
      // return;
      if (
        ["인공증식증명서", "양도양수확인서", "폐사질병신고서"].includes(
          documentType
        )
      ) {
        setData(data);
        setLayout(documentType);
        return true;
      } else {
        setData(null);
        setLayout(null);
        return false;
      }

      // 실패했을 시에 실패 알림 추가할 것
    };
    getData();
  }, []);

  const handleAcceptSubmit = (turtleUUID: string, documentHash: string) => {
    approveDocumentRequest(turtleUUID, documentHash, true).then((response) => {
      if (response.success) {
        alert("서류 승인 처리가 완료되었습니다.");
        navigate("/admin/document/list");
      } else {
        alert("서류 승인 처리에 실패했습니다. 다시 시도해 주세요.");
      }
    });
  };
  const handleDenySubmit = (turtleUUID: string, documentHash: string) => {
    approveDocumentRequest(turtleUUID, documentHash, false).then((response) => {
      if (response.success) {
        alert("서류 반려 처리가 완료되었습니다.");
        navigate("/admin/document/list");
      } else {
        alert("서류 반려 처리에 실패했습니다. 다시 시도해 주세요.");
      }
    });
  };

  return (
    <>
      <Helmet>
        <title>관리자 - 문서 상세 조회</title>
      </Helmet>
      <Header />

      {/* 테스트 드라이버 */}
      {/* <div className="space-x-3 text-center">
        <button
          onClick={() => {
            setLayout("인공증식증명서");
            setData(adminBreedResultdata);
          }}
        >
          증식
        </button>
        <button
          onClick={() => {
            setLayout("양도양수확인서");
            setData(adminAssignGrantData);
          }}
        >
          양수
        </button>

        <button
          onClick={() => {
            setLayout("폐사질병신고서");
            setData(adminDeathData);
          }}
        >
          폐사
        </button>
      </div> */}
      {/* 테스트 드라이버 끝 */}

      <div className="mt-[100px]">
        {layout === "인공증식증명서" && (
          <AdminBreedDocsCheck
            onAccept={handleAcceptSubmit}
            onDeny={handleDenySubmit}
            data={data as AdminBreedDocumentDataType}
          />
        )}

        {/* 양수&양도는 같은 양식을 사용함. */}
        {layout === "양도양수확인서" && (
          <AdminAssignGrantDocsCheck
            onAccept={handleAcceptSubmit}
            onDeny={handleDenySubmit}
            data={data as AdminAssignDocumentDataType}
          />
        )}
        {layout === "폐사질병신고서" && (
          <AdminDeathDocsCheck
            onAccept={handleAcceptSubmit}
            onDeny={handleDenySubmit}
            data={data as AdminDeathDocumentDataType}
          />
        )}
      </div>
    </>
  );
}

export default AdminDocsDetailPage;
